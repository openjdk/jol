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
package org.openjdk.jol.info;

import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.layouters.CurrentLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.util.ClassUtils;
import org.openjdk.jol.util.ObjectUtils;
import org.openjdk.jol.vm.VM;
import org.openjdk.jol.vm.VirtualMachine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.SortedSet;

/**
 * Handles the class data *with* the layout information.
 */
public class ClassLayout {

    /**
     * Produce the class layout for the given class.
     *
     * This is a shortcut for {@link #parseClass(Class,org.openjdk.jol.layouters.Layouter)},
     * but with a default layouter.
     *
     * @param klass class to work on
     * @return class layout
     */
    public static ClassLayout parseClass(Class<?> klass) {
        return parseClass(klass, new CurrentLayouter());
    }

    /**
     * Produce the class layout for the given class, and given layouter.
     *
     * Note: this method is usable as the "caching" shortcut for {@link #parseInstance(Object)}.
     * You can use it to cache the introspection results for a constant-sized
     * objects, e.g. plain Java objects. It is not recommended to use this method
     * on arrays, since their lengths differ from instance to instance.
     *
     * @param klass    class to work on
     * @param layouter class layouter
     * @return class layout
     */
    public static ClassLayout parseClass(Class<?> klass, Layouter layouter) {
        return layouter.layout(ClassData.parseClass(klass));
    }

    /**
     * Produce the class layout for the given instance.
     *
     * This is a shortcut for {@link #parseInstance(java.lang.Object,org.openjdk.jol.layouters.Layouter)},
     * but with a default layouter.
     *
     * @param instance instance to work on
     * @return class layout
     */
    public static ClassLayout parseInstance(Object instance) {
        return parseInstance(instance, new CurrentLayouter());
    }

    /**
     * Produce the class layout for the given instance, and given layouter.
     *
     * These methods, along with {@link #parseInstance(Object)} are recommended
     * for use when the shape of the object is not known in advance. For example,
     * variable-sized instances (e.g. Java arrays) would not be parsed by
     * {@link #parseClass(Class)} properly, because their lengths are encoded in
     * the instance objects, not in classes.
     *
     * @param instance instance to work on
     * @param layouter class layouter
     * @return class layout
     */
    public static ClassLayout parseInstance(Object instance, Layouter layouter) {
        return layouter.layout(ClassData.parseInstance(instance));
    }

    private final ClassData classData;
    private final boolean isArray;
    private final SortedSet<FieldLayout> fields;
    private final DataModel model;
    private final long size;
    private final int lossesInternal;
    private final int lossesExternal;
    private final int lossesTotal;

    private ClassLayout(ClassData classData, SortedSet<FieldLayout> fields, DataModel model, long instanceSize, int lossesInternal, int lossesExternal, int lossesTotal) {
        this.classData = classData;
        this.fields = fields;
        this.model = model;
        this.size = instanceSize;
        this.lossesInternal = lossesInternal;
        this.lossesExternal = lossesExternal;
        this.lossesTotal = lossesTotal;
        this.isArray = classData.isArray();
    }

    /**
     * Builds the class layout.
     *
     * @param classData    class data
     * @param fields       field layouts
     * @param model        data model to use
     * @param instanceSize instance size
     * @param check        whether to check important invariants
     * @return a new instance of the ClassLayout
     */
    public static ClassLayout create(ClassData classData, SortedSet<FieldLayout> fields, DataModel model, long instanceSize, boolean check) {
        if (check) {
            checkInvariants(fields, instanceSize);
        }
        // calculate loses
        long next = classData.isArray() ? model.arrayHeaderSize() : model.headerSize();
        long internal = 0;
        for (FieldLayout fl : fields) {
            if (fl.offset() > next) {
                internal += fl.offset() - next;
            }
            next = fl.offset() + fl.size();
        }
        long external = (instanceSize != next) ? (instanceSize - next) : 0;
        long total = internal + external;
        return new ClassLayout(classData, fields, model, instanceSize, (int) internal, (int) external, (int) total);
    }

    private static void checkInvariants(SortedSet<FieldLayout> fields, long instanceSize) {
        FieldLayout lastField = null;
        for (FieldLayout f : fields) {
            if (f.offset() % f.size() != 0) {
                throw new IllegalStateException("Field " + f + " is not aligned");
            }
            if (f.offset() + f.size() > instanceSize) {
                throw new IllegalStateException("Field " + f + " is overflowing the object of size " + instanceSize);
            }
            if (lastField != null && (f.offset() < lastField.offset() + lastField.size())) {
                throw new IllegalStateException("Field " + f + " overlaps with the previous field "+ lastField);
            }
            lastField = f;
        }
    }

    /**
     * Answer the set of fields, including those in superclasses
     *
     * @return sorted set of fields
     */
    public SortedSet<FieldLayout> fields() {
        return fields;
    }

    /**
     * Answer instance size
     *
     * @return instance size
     */
    public long instanceSize() {
        return size;
    }

    /**
     * Answer header size
     *
     * @return header size
     */
    public int headerSize() {
        return isArray ? model.arrayHeaderSize() : model.headerSize();
    }

    /**
     * Loosed bytes from padding between fields
     *
     * @return Internally loosed bytes
     */
    public long getLossesInternal() {
        return lossesInternal;
    }

    /**
     * Loosed bytes due to next object alignment
     *
     * @return Externally loosed bytes
     */
    public long getLossesExternal() {
        return lossesExternal;
    }

    /**
     * Total loosed bytes i.e. lossesInternal + lossesExternal
     *
     * @return Total loosed bytes
     */
    public long getLossesTotal() {
        return lossesTotal;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (FieldLayout f : fields()) {
            sb.append(f).append("\n");
        }
        sb.append("size = ").append(size).append("\n");
        return sb.toString();
    }

    /**
     * Produce printable stringly representation of class layout.
     * This method uses the instance originally provided to {@link #parseInstance(Object)},
     * if that instance is still available.
     *
     * @return human-readable layout info
     */
    public String toPrintable() {
        return toPrintable(classData.instance());
    }

    /**
     * Produce printable stringly representation of class layout.
     * This method accepts instance to read the actual data from.
     *
     * @param instance instance to work on
     * @return human-readable layout info
     */
    public String toPrintable(Object instance) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        int maxTypeLen = "TYPE".length();
        for (FieldLayout f : fields()) {
            maxTypeLen = Math.max(f.typeClass().length(), maxTypeLen);
        }
        maxTypeLen += 2;

        String MSG_OBJ_HEADER = "(object header)";
        String MSG_MARK_WORD = "(object header: mark)";
        String MSG_CLASS_WORD = "(object header: class)";
        String MSG_ARR_LEN = "(array length)";
        String MSG_FIELD_GAP = "(alignment/padding gap)";
        String MSG_OBJ_GAP = "(object alignment gap)";

        int maxDescrLen = "DESCRIPTION".length();
        maxDescrLen = Math.max(maxDescrLen, MSG_OBJ_HEADER.length());
        maxDescrLen = Math.max(maxDescrLen, MSG_MARK_WORD.length());
        maxDescrLen = Math.max(maxDescrLen, MSG_CLASS_WORD.length());
        maxDescrLen = Math.max(maxDescrLen, MSG_FIELD_GAP.length());
        maxDescrLen = Math.max(maxDescrLen, MSG_OBJ_GAP.length());
        for (FieldLayout f : fields()) {
            maxDescrLen = Math.max(f.shortFieldName().length(), maxDescrLen);
        }
        maxDescrLen += 2;

        String format  = "%3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n";
        String formatS = "%3s %3s %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n";

        if (instance != null) {
            try {
                Class<?> klass = ClassUtils.loadClass(classData.name());
                if (!klass.isAssignableFrom(instance.getClass())) {
                    throw new IllegalArgumentException("Passed instance type " + instance.getClass() + " is not assignable from " + klass + ".");
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Class is not found: " + classData.name() + ".");
            }
        }

        pw.println(classData.name() + " object internals:");
        pw.printf(formatS, "OFF", "SZ", "TYPE", "DESCRIPTION", "VALUE");

        String markStr = "N/A";
        String classStr = "N/A";
        String arrLenStr = "N/A";

        int markSize = model.markHeaderSize();
        int classSize = model.classHeaderSize();
        int arrSize = model.arrayLengthHeaderSize();

        int markOffset = 0;
        int classOffset = markOffset + markSize;
        int arrOffset = classOffset + classSize;

        if (instance != null) {
            VirtualMachine vm = VM.current();
            if (markSize == 8) {
                long mark = vm.getLong(instance, markOffset);
                String decoded = (classSize > 0) ? parseMarkWord(mark) : "(Lilliput)";
                markStr = toHex(mark) + " " + decoded;
            } else if (markSize == 4) {
                int mark = vm.getInt(instance, markOffset);
                String decoded = (classSize > 0) ? parseMarkWord(mark) : "(Lilliput)";
                markStr = toHex(mark) + " " + decoded;
            }

            if (classSize == 8) {
                classStr = toHex(vm.getLong(instance, classOffset));
            } else if (classSize == 4) {
                classStr = toHex(vm.getInt(instance, classOffset));
            }

            if (classData.isArray()) {
                arrLenStr = Integer.toString(vm.getInt(instance, arrOffset));
            }
        }

        pw.printf(format, markOffset, markSize, "", MSG_MARK_WORD, markStr);
        if (classSize > 0) {
            pw.printf(format, classOffset, classSize, "", MSG_CLASS_WORD, classStr);
        }
        if (classData.isArray()) {
            pw.printf(format, arrOffset, arrSize, "", MSG_ARR_LEN, arrLenStr);
        }

        long nextFree = headerSize();

        for (FieldLayout f : fields()) {
            if (f.offset() > nextFree) {
                pw.printf(format, nextFree, (f.offset() - nextFree), "", MSG_FIELD_GAP, "");
            }

            Field fi = f.data().refField();
            pw.printf(format,
                    f.offset(),
                    f.size(),
                    f.typeClass(),
                    f.shortFieldName(),
                    (instance != null && fi != null) ? ObjectUtils.safeToString(ObjectUtils.value(instance, fi)) : "N/A"
            );

            nextFree = f.offset() + f.size();
        }

        long sizeOf = (instance != null) ? VM.current().sizeOf(instance) : instanceSize();
        if (sizeOf != nextFree) {
            pw.printf(format, nextFree, lossesExternal, "", MSG_OBJ_GAP, "");
        }

        pw.printf("Instance size: %d bytes%n", sizeOf);
        pw.printf("Space losses: %d bytes internal + %d bytes external = %d bytes total%n", lossesInternal, lossesExternal, lossesTotal);

        pw.close();

        return sw.toString();
    }

    static final String[] ZERO_RUNS;

    static {
        ZERO_RUNS = new String[16];
        String s = "";
        for (int c = 0; c < ZERO_RUNS.length; c++) {
            ZERO_RUNS[c] = s;
            s += "0";
        }
    }

    private static String toHex(int x) {
        String s = Integer.toHexString(x);
        int deficit = 8 - s.length();
        return "0x" + ZERO_RUNS[deficit] + s;
    }

    private static String toHex(long x) {
        String s = Long.toHexString(x);
        int deficit = 16 - s.length();
        return "0x" + ZERO_RUNS[deficit] + s;
    }

    private static String parseMarkWord(int mark) {
        // 32 bits:
        //    hash:25 ------------>| age:4    biased_lock:1 lock:2 (normal object)
        //    JavaThread*:23 epoch:2 age:4    biased_lock:1 lock:2 (biased object)
        int bits = mark & 0b11;
        switch (bits) {
            case 0b11:
                return "(marked: " + toHex(mark) + ")";
            case 0b00:
                return "(thin lock: " + toHex(mark) + ")";
            case 0b10:
                return "(fat lock: " + toHex(mark) + ")";
            case 0b01: // other
                String s = "; age: " + ((mark >> 3) & 0xF);
                int tribits = mark & 0b111;
                switch (tribits) {
                    case 0b001:
                        int hash = mark >>> 7;
                        if (hash != 0) {
                            return "(hash: " + toHex(hash) + s + ")";
                        } else {
                            return "(non-biasable" + s + ")";
                        }
                    case 0b101:
                        int thread = mark >>> 9;
                        if (thread == 0) {
                            return "(biasable" + s + ")";
                        } else {
                            return "(biased: " + toHex(thread) + "; epoch: " + ((mark >> 7) & 0x2) + s + ")";
                        }
                }
            default:
                return "(parse error)";
        }
    }

    private static String parseMarkWord(long mark) {
        //  64 bits:
        //  unused:25 hash:31 -->| unused_gap:1   age:4    biased_lock:1 lock:2 (normal object)
        //  JavaThread*:54 epoch:2 unused_gap:1   age:4    biased_lock:1 lock:2 (biased object)
        long bits = mark & 0b11;
        switch ((int) bits) {
            case 0b11:
                return "(marked: " + toHex(mark) + ")";
            case 0b00:
                return "(thin lock: " + toHex(mark) + ")";
            case 0b10:
                return "(fat lock: " + toHex(mark) + ")";
            case 0b01:
                String s = "; age: " + ((mark >> 3) & 0xF);
                int tribits = (int) (mark & 0b111);
                switch (tribits) {
                    case 0b001:
                        int hash = (int)(mark >>> 8);
                        if (hash != 0) {
                            return "(hash: " + toHex(hash) + s + ")";
                        } else {
                            return "(non-biasable" + s + ")";
                        }
                    case 0b101:
                        long thread = mark >>> 10;
                        if (thread == 0) {
                            return "(biasable" + s + ")";
                        } else {
                            return "(biased: " + toHex(thread) + "; epoch: " + ((mark >> 8) & 0x2) + s + ")";
                        }
                }
            default:
                return "(parse error)";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassLayout that = (ClassLayout) o;
        return fields.equals(that.fields) &&
                model.equals(that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, model);
    }
}
