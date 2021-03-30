/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jol.layouters;

import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.info.FieldLayout;
import org.openjdk.jol.util.MathUtil;

import java.lang.IllegalStateException;
import java.util.*;

import static org.openjdk.jol.layouters.FieldAllocationType.*;


/**
 * VM layout simulator.
 *
 * @author Aleksey Shipilev
 */
public class HotSpotLayouter implements Layouter {
    // The next classes have predefined hard-coded fields offsets.
    private static Set<String> PREDEF_OFFSETS = new HashSet<>(Arrays.asList(
            "java.lang.AssertionStatusDirectives",
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.ref.Reference",
            "java.lang.ref.SoftReference",
            "java.lang.StackTraceElement",
            "java.lang.String",
            "java.lang.Throwable",
            "java.lang.Boolean",
            "java.lang.Character",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long"
    ));

    static final int CONTENDED_PADDING_WIDTH = Integer.getInteger("contendedPaddingWidth", 128);

    private final DataModel model;
    private final int jdkVersion;

    public HotSpotLayouter(DataModel model, int jdkVersion) {
        this.model = model;
        this.jdkVersion = jdkVersion;
    }

    @Override
    public ClassLayout layout(ClassData cd) {
        if (cd.isArray()) {
            // special case for arrays
            int base = model.arrayHeaderSize();
            int scale = model.sizeOf(cd.arrayComponentType());

            long instanceSize = base + cd.arrayLength() * scale;
            instanceSize = MathUtil.align(instanceSize, model.objectAlignment());
            base = MathUtil.align(base, Math.max(4, scale));

            SortedSet<FieldLayout> result = new TreeSet<>();
            result.add(new FieldLayout(FieldData.create(cd.arrayClass(), "<elements>", cd.arrayComponentType()), base, scale * cd.arrayLength()));
            return ClassLayout.create(cd, result, model, instanceSize, false);
        }

        if (jdkVersion >= 15) {
            return newLayouter(cd);
        } else {
            return oldLayouter(cd);
        }
    }

    private ClassLayout newLayouter(ClassData cd) {
        SortedSet<FieldLayout> result = new TreeSet<>();

        List<String> hierarchy = cd.classHierarchy();

        BitSet claimed = new BitSet();
        claimed.set(0, model.headerSize());

        for (String k : hierarchy) {
            Collection<FieldData> fields = cd.fieldsFor(k);

            SortedSet<FieldLayout> current = new TreeSet<>();
            for (int size : new int[]{8, 4, 2, 1}) {
                for (FieldData f : fields) {
                    if (!f.isPrimitive()) continue;
                    int fSize = model.sizeOf(f.typeClass());
                    if (fSize != size) continue;

                    for (int t = 0; t < Integer.MAX_VALUE; t++) {
                        if (claimed.get(t * size, (t + 1) * size).isEmpty()) {
                            claimed.set(t * size, (t + 1) * size);
                            current.add(new FieldLayout(f, t * size, size));
                            break;
                        }
                    }
                }
            }
            for (int size : new int[]{8, 4, 2, 1}) {
                for (FieldData f : fields) {
                    if (f.isPrimitive()) continue;
                    int fSize = model.sizeOf(f.typeClass());
                    if (fSize != size) continue;

                    for (int t = 0; t < Integer.MAX_VALUE; t++) {
                        if (claimed.get(t * size, (t + 1) * size).isEmpty()) {
                            claimed.set(t * size, (t + 1) * size);
                            current.add(new FieldLayout(f, t * size, size));
                            break;
                        }
                    }
                }
            }
            result.addAll(current);
        }

        int instanceSize = MathUtil.align(claimed.length(), model.objectAlignment());

        return ClassLayout.create(cd, result, model, instanceSize, true);
    }

    private ClassLayout oldLayouter(ClassData cd) {
        SortedSet<FieldLayout> result = new TreeSet<>();

        List<ClassData> classDataClassHierarchy = new ArrayList<>();
        ClassData cld = cd;
        classDataClassHierarchy.add(cld);

        while ((cld = cld.superClass()) != null) {
            classDataClassHierarchy.add(0, cld);
        }

        int superClassFieldsSize = 0;
        int nextPaddedOffset = 0;

        for (ClassData clsData : classDataClassHierarchy) {
            EnumMap<FieldAllocationType, Integer> fieldsAllocationCount = new EnumMap<>(FieldAllocationType.class);
            EnumMap<FieldAllocationType, Integer> nextOffset = new EnumMap<>(FieldAllocationType.class);
            EnumMap<FieldAllocationType, ArrayDeque<Integer>> spaceOffset = new EnumMap<>(FieldAllocationType.class);
            EnumMap<FieldAllocationType, Integer> allocationTypeSizes = new EnumMap<>(FieldAllocationType.class);

            for (FieldAllocationType atype : FieldAllocationType.values()) {
                fieldsAllocationCount.put(atype, 0);
                nextOffset.put(atype,  0);
                spaceOffset.put(atype, new ArrayDeque<Integer>());
            }
            allocationTypeSizes.put(OOP,    model.sizeOf("oop"));
            allocationTypeSizes.put(BYTE,   model.sizeOf("byte"));
            allocationTypeSizes.put(SHORT,  model.sizeOf("short"));
            allocationTypeSizes.put(WORD,   model.sizeOf("int"));
            allocationTypeSizes.put(DOUBLE, model.sizeOf("long"));

            for (FieldData f : clsData.ownFields()) {
                FieldAllocationType atype = FieldAllocationType.allocationTypeFor(f);
                Integer count = fieldsAllocationCount.get(atype);
                fieldsAllocationCount.put(atype, ++count);
            }

            // Count the contended fields by type.
            int contendedCount = 0;
            EnumMap<FieldAllocationType, Integer> facContended = new EnumMap<>(FieldAllocationType.class);

            for (FieldData f : clsData.ownFields()) {
                FieldAllocationType atype = FieldAllocationType.allocationTypeFor(f);
                if (f.isContended()) {
                    Integer count = facContended.get(atype);
                    facContended.put(atype, count == null ? 1 : ++count);
                    contendedCount++;
                }
            }

            int nextFieldOffset = (clsData.superClass() == null ? model.headerSize() : 0) + superClassFieldsSize;

            boolean isContendedClass = clsData.isContended();

            // Class is contended, pad before all the fields
            if (isContendedClass) {
                nextFieldOffset += CONTENDED_PADDING_WIDTH;
            }

            // Compute the non-contended fields count.
            // The packing code below relies on these counts to determine if some field
            // can be squeezed into the alignment gap. Contended fields are obviously
            // exempt from that.
            int doubleCount = fieldsAllocationCount.get(DOUBLE) - (facContended.containsKey(DOUBLE) ? facContended.get(DOUBLE) : 0);
            int wordCount   = fieldsAllocationCount.get(WORD)   - (facContended.containsKey(WORD)   ? facContended.get(WORD)   : 0);
            int shortCount  = fieldsAllocationCount.get(SHORT)  - (facContended.containsKey(SHORT)  ? facContended.get(SHORT)  : 0);
            int byteCount   = fieldsAllocationCount.get(BYTE)   - (facContended.containsKey(BYTE)   ? facContended.get(BYTE)   : 0);
            int oopCount    = fieldsAllocationCount.get(OOP)    - (facContended.containsKey(OOP)    ? facContended.get(OOP)    : 0);

            int firstOopOffset = 0; // will be set for first oop field

            boolean compactFields = true;
            int allocationStyle = 1;

            // Use default fields allocation order for classes, which have predefined hard-coded fields offsets.
            if (PREDEF_OFFSETS.contains(clsData.name())) {
                allocationStyle = 0;   // Allocate oops first
                compactFields = false; // Don't compact fields
            }

            // Rearrange fields for a given allocation style
            if (allocationStyle == 0) {
                // Fields order: oops, longs/doubles, ints, shorts/chars, bytes, padded fields
                nextOffset.put(OOP, nextFieldOffset);
                nextOffset.put(DOUBLE, nextOffset.get(OOP) + (oopCount * model.sizeOf("oop")));
            } else {
                // Fields order: longs/doubles, ints, shorts/chars, bytes, oops, padded fields
                nextOffset.put(DOUBLE, nextFieldOffset);
            }

            // Try to squeeze some of the fields into the gaps due to
            // long/double alignment.
            if (doubleCount > 0) {
                int offset = nextOffset.get(DOUBLE);
                nextOffset.put(DOUBLE, MathUtil.align(offset, allocationTypeSizes.get(DOUBLE)));
                if (offset != nextOffset.get(DOUBLE)) {
                    int length = nextOffset.get(DOUBLE) - offset;

                    // Allocate available fields into the gap before double field.
                    if (compactFields) {
                        if (wordCount > 0) {
                            wordCount -= 1;
                            spaceOffset.get(WORD).push(offset);
                            length -= allocationTypeSizes.get(WORD);
                            offset += allocationTypeSizes.get(WORD);
                        }
                        while (length >= allocationTypeSizes.get(SHORT) && shortCount > 0) {
                            shortCount -= 1;
                            spaceOffset.get(SHORT).push(offset);
                            length -= allocationTypeSizes.get(SHORT);
                            offset += allocationTypeSizes.get(SHORT);
                        }
                        while (length > 0 && byteCount > 0) {
                            byteCount -= 1;
                            spaceOffset.get(BYTE).push(offset);
                            length -= allocationTypeSizes.get(BYTE);
                            offset += allocationTypeSizes.get(BYTE);
                        }
                        // Allocate oop field in the gap if there are no other fields for that.
                        if (length >= allocationTypeSizes.get(OOP) && oopCount > 0) {
                            // when oop fields not first
                            oopCount -= 1;
                            spaceOffset.get(OOP).push(offset);
                        }
                    }
                }
            }

            nextOffset.put(WORD,  nextOffset.get(DOUBLE) + (doubleCount * allocationTypeSizes.get(DOUBLE)));
            nextOffset.put(SHORT, nextOffset.get(WORD)   +   (wordCount * allocationTypeSizes.get(WORD)));
            nextOffset.put(BYTE,  nextOffset.get(SHORT)  +  (shortCount * allocationTypeSizes.get(SHORT)));
            nextPaddedOffset = nextOffset.get(BYTE) + byteCount;

            // let oops jump before padding with this allocation style
            if (allocationStyle == 1) {
                nextOffset.put(OOP, nextPaddedOffset);
                if (oopCount > 0) {
                    nextOffset.put(OOP, MathUtil.align(nextOffset.get(OOP), allocationTypeSizes.get(OOP)));
                }
                nextPaddedOffset = nextOffset.get(OOP) + (oopCount * allocationTypeSizes.get(OOP));
            }

            Set<FieldData> layoutedFields = new HashSet<>();

            // Iterate over fields again and compute correct offsets.
            // The field allocation type was temporarily stored in the offset slot.
            // oop fields are located before non-oop fields.
            for (FieldData f : clsData.ownFields()) {

                // skip already laid out fields
                if (layoutedFields.contains(f)) continue;

                // contended instance fields are handled below
                if (f.isContended()) continue;

                FieldAllocationType atype = FieldAllocationType.allocationTypeFor(f);
                int allocationTypeSize = allocationTypeSizes.get(atype);
                Integer allocationTypeSpaceOffset = spaceOffset.get(atype).poll();

                // pack the rest of the fields
                int realOffset;
                if (atype == DOUBLE) {
                    int nextDoubleOffset = nextOffset.get(DOUBLE);
                    realOffset = nextOffset.get(DOUBLE);
                    nextOffset.put(atype, nextDoubleOffset + allocationTypeSize);
                } else {
                    if (allocationTypeSpaceOffset != null) {
                        realOffset = allocationTypeSpaceOffset;
                    } else {
                        int allocationTypeNextOffset = nextOffset.get(atype);
                        realOffset = allocationTypeNextOffset;
                        nextOffset.put(atype, allocationTypeNextOffset + allocationTypeSize);
                    }
                }

                layoutedFields.add(f);
                result.add(new FieldLayout(f, realOffset, model.sizeOf(f.typeClass())));
            }

            // Handle the contended cases.
            //
            // Each contended field should not intersect the cache line with another contended field.
            // In the absence of alignment information, we end up with pessimistically separating
            // the fields with full-width padding.
            //
            // Additionally, this should not break alignment for the fields, so we round the alignment up
            // for each field.
            if (contendedCount > 0) {

                // if there is at least one contended field, we need to have pre-padding for them
                nextPaddedOffset += CONTENDED_PADDING_WIDTH;

                // collect all contended groups
                HashSet<String> contendedGroups = new HashSet<>();

                for (FieldData f : clsData.ownFields()) {
                    if (f.isContended()) {
                        contendedGroups.add(f.contendedGroup());
                    }
                }

                for (String currentGroup : contendedGroups) {

                    for (FieldData f : clsData.ownFields()) {

                        // skip already laid out fields
                        if (layoutedFields.contains(f)) continue;

                        // skip non-contended fields and fields from different group
                        if (!f.isContended() || !f.contendedGroup().equals(currentGroup)) continue;

                        FieldAllocationType atype = FieldAllocationType.allocationTypeFor(f);

                        int allocationTypeSize = allocationTypeSizes.get(atype);
                        nextPaddedOffset = MathUtil.align(nextPaddedOffset, allocationTypeSize);
                        int realOffset = nextPaddedOffset;
                        nextPaddedOffset += allocationTypeSize;

                        if (atype == OOP && firstOopOffset == 0) { // Undefined
                            firstOopOffset = realOffset;
                        }

                        if (f.contendedGroup().equals("")) {
                            // Contended group defines the equivalence class over the fields:
                            // the fields within the same contended group are not inter-padded.
                            // The only exception is default group, which does not incur the
                            // equivalence, and so requires intra-padding.
                            nextPaddedOffset += CONTENDED_PADDING_WIDTH;
                        }

                        result.add(new FieldLayout(f, realOffset, model.sizeOf(f.typeClass())));
                    }

                    // Start laying out the next group.
                    // Note that this will effectively pad the last group in the back;
                    // this is expected to alleviate memory contention effects for
                    // subclass fields and/or adjacent object.
                    // If this was the default group, the padding is already in place.
                    if (!currentGroup.equals("")) {
                        nextPaddedOffset += CONTENDED_PADDING_WIDTH;
                    }
                }
            }

            // Entire class is contended, pad in the back.
            // This helps to alleviate memory contention effects for subclass fields
            // and/or adjacent object.
            if (isContendedClass) {
                nextPaddedOffset += CONTENDED_PADDING_WIDTH;
            }

            superClassFieldsSize = MathUtil.align(nextPaddedOffset, model.sizeOf("oop"));
        }

        int minAlignment = model.objectAlignment();
        for (String k : cd.classHierarchy()) {
            Collection<FieldData> fields = cd.fieldsFor(k);
            for (FieldData f : fields) {
                minAlignment = Math.max(minAlignment, model.sizeOf(f.typeClass()));
            }
        }

        int instanceSize = MathUtil.align(nextPaddedOffset, minAlignment);

        return ClassLayout.create(cd, result, model, instanceSize, true);
    }

    @Override
    public String toString() {
        return "Hotspot Layout Simulation (JDK " + jdkVersion + ", " + model + ")";
    }
}
