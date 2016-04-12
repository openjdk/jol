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

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * VM layout simulator.
 *
 * @author Aleksey Shipilev
 */
public class HotSpotLayouter implements Layouter {
    private final DataModel model;
    private final boolean takeHierarchyGaps;
    private final boolean takeSuperGaps;
    private final boolean autoAlign;

    public HotSpotLayouter(DataModel model) {
        this(model, false, false, false);
    }

    public HotSpotLayouter(DataModel model, boolean takeHierarchyGaps, boolean takeSuperGaps, boolean autoAlign) {
        this.model = model;
        this.takeHierarchyGaps = takeHierarchyGaps;
        this.takeSuperGaps = takeSuperGaps;
        this.autoAlign = autoAlign;
    }

    @Override
    public ClassLayout layout(ClassData cd) {
        SortedSet<FieldLayout> result = new TreeSet<FieldLayout>();

        if (cd.isArray()) {
            // special case for arrays
            int base = model.arrayHeaderSize();
            int scale = model.sizeOf(cd.arrayComponentType());

            long instanceSize = base + cd.arrayLength() * scale;
            instanceSize = MathUtil.align(instanceSize, autoAlign ? Math.max(model.objectAlignment(), scale) : model.objectAlignment());
            base = MathUtil.align(base, Math.max(4, scale));

            result.add(new FieldLayout(FieldData.create(cd.arrayClass(), "<elements>", cd.arrayComponentType()), base, scale * cd.arrayLength()));
            return new ClassLayout(cd, result, model.arrayHeaderSize(), instanceSize, false);
        }

        List<String> hierarchy = cd.classHierarchy();

        BitSet claimed = new BitSet();

        claimed.set(0, model.headerSize());

        for (String k : hierarchy) {

            Collection<FieldData> fields = cd.fieldsFor(k);

            SortedSet<FieldLayout> current = new TreeSet<FieldLayout>();
            for (int size : new int[]{8, 4, 2, 1}) {
                for (FieldData f : fields) {
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

            if (takeSuperGaps) {
                // do nothing
            } else if (takeHierarchyGaps) {
                // claim only the class body up to the field
                int lastSet = claimed.length();
                claimed.set(0, lastSet);
            } else {
                // claim the entire class body, plus some alignment
                int lastSet = claimed.length();
                claimed.set(0, MathUtil.align(lastSet, model.sizeOf("java.lang.Object")));
            }
        }

        int instanceSize;
        if (autoAlign) {
            int a = 4;
            for (FieldLayout f : result) {
                a = Math.max(a, model.sizeOf(f.typeClass()));
            }
            instanceSize = MathUtil.align(claimed.length(), a);
        } else {
            instanceSize = MathUtil.align(claimed.length(), model.objectAlignment());
        }

        return new ClassLayout(cd, result, model.headerSize(), instanceSize, true);
    }

    @Override
    public String toString() {
        return "VM Layout Simulation (" + model
                + (takeHierarchyGaps ? ", hierarchy gaps" : "")
                + (takeSuperGaps ? ", super gaps" : "")
                + (autoAlign ? ", autoalign" : "") + ")";
    }
}
