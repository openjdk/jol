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

import org.openjdk.jol.layouters.CurrentLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.util.ClassUtils;
import org.openjdk.jol.util.ObjectUtils;
import org.openjdk.jol.vm.VM;
import org.openjdk.jol.vm.VirtualMachine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
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
    private final SortedSet<FieldLayout> fields;
    private final int headerSize;
    private final long size;
    private final int lossesInternal;
    private final int lossesExternal;
    private final int lossesTotal;

    private ClassLayout(ClassData classData, SortedSet<FieldLayout> fields, int headerSize, long instanceSize, int lossesInternal, int lossesExternal, int lossesTotal) {
        this.classData = classData;
        this.fields = fields;
        this.headerSize = headerSize;
        this.size = instanceSize;
        this.lossesInternal = lossesInternal;
        this.lossesExternal = lossesExternal;
        this.lossesTotal = lossesTotal;
    }

    /**
     * Builds the class layout.
     *
     * @param classData    class data
     * @param fields       field layouts
     * @param headerSize   header size
     * @param instanceSize instance size
     * @param check        whether to check important invariants
     * @return a new instance of the ClassLayout
     */
    public static ClassLayout create(ClassData classData, SortedSet<FieldLayout> fields, int headerSize, long instanceSize, boolean check) {
        if (check) {
            checkInvariants(fields, instanceSize);
        }
        // calculate loses
        long next = headerSize;
        long internal = 0;
        for (FieldLayout fl : fields) {
            if (fl.offset() > next) {
                internal += fl.offset() - next;
            }
            next = fl.offset() + fl.size();
        }
        long external = (instanceSize != next) ? (instanceSize - next) : 0;
        long total = internal + external;
        return new ClassLayout(classData, fields, headerSize, instanceSize, (int) internal, (int) external, (int) total);
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
        return headerSize;
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
        String MSG_MARK_WORD = "(mark word)";
        String MSG_CLASS_WORD = "(class word)";
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
        pw.printf(" %3s %3s %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n", "OFF", "SZ", "TYPE", "DESCRIPTION", "VALUE");
        if (instance != null) {
            VirtualMachine vm = VM.current();

            if (vm.addressSize() == 4) {
                // 32-bit VM
                int mark  = vm.getInt(instance, 0);
                pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n",
                        0, 4, "", MSG_MARK_WORD, toHex(mark) + ": " + parseMarkWord(mark));
                int klass = vm.getInt(instance, 4);
                pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n",
                        4, 4, "", MSG_CLASS_WORD, toHex(klass));
                if (classData.isArray()) {
                    int len = vm.getInt(instance, 8);
                    pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n",
                            8, 4, "", "(array length)", toHex(len));
                }
            } else if (vm.addressSize() == 8) {
                // 64-bit VM
                long mark = vm.getLong(instance, 0);
                pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n",
                        0, 4, "", MSG_MARK_WORD, toHex(mark) + ": " + parseMarkWord(mark));
                if (vm.compressedKlassPtrs()) {
                    int klass = vm.getInt(instance, 8);
                    pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n",
                            8, 4, "", MSG_CLASS_WORD, toHex(klass));
                } else {
                    long klass = vm.getLong(instance, 8);
                    pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n",
                            8, 8, "klass", MSG_CLASS_WORD, toHex(klass));
                }
                if (classData.isArray()) {
                    int off = vm.compressedKlassPtrs() ? 12 : 16;
                    int len = vm.getInt(instance, off);
                    pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n",
                            off, 4, "", MSG_ARR_LEN, len);
                }
            } else {
                for (long off = 0; off < headerSize(); off += 4) {
                    int word = vm.getInt(instance, off);
                    pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n",
                            off, 4, "", MSG_OBJ_HEADER, toHex(word));
                }
            }
        } else {
            pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n", 0, headerSize(), "", MSG_OBJ_HEADER, "N/A");
        }

        long nextFree = headerSize();

        for (FieldLayout f : fields()) {
            if (f.offset() > nextFree) {
                pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s%n", nextFree, (f.offset() - nextFree), "", MSG_FIELD_GAP);
            }

            Field fi = f.data().refField();
            pw.printf(" %3d %3d %" + maxTypeLen + "s %-" + maxDescrLen + "s %s%n",
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
            pw.printf(" %3d %3s %" + maxTypeLen + "s %s%n", nextFree, lossesExternal, "", MSG_OBJ_GAP);
        }

        pw.printf("Instance size: %d bytes%n", sizeOf);
        pw.printf("Space losses: %d bytes internal + %d bytes external = %d bytes total%n", lossesInternal, lossesExternal, lossesTotal);

        pw.close();

        return sw.toString();
    }

    // very ineffective, so what?
    private static String toHex(int x) {
        String s = Integer.toHexString(x);
        int deficit = 4 - s.length();
        for (int c = 0; c < deficit; c++) {
            s = "0" + s;
        }
        return "0x" + s;
    }

    // very ineffective, so what?
    private static String toHex(long x) {
        String s = Long.toHexString(x);
        int deficit = 8 - s.length();
        for (int c = 0; c < deficit; c++) {
            s = "0" + s;
        }
        return "0x" + s;
    }

    private static String parseMarkWord(int mark) {
        // 32 bits:
        //    hash:25 ------------>| age:4    biased_lock:1 lock:2 (normal object)
        //    JavaThread*:23 epoch:2 age:4    biased_lock:1 lock:2 (biased object)
        int bits = mark & 0b11;
        switch (bits) {
            case 0b11:
                return "marked(" + toHex(mark) + ")";
            case 0b10: // has monitor
                return "monitor(" + toHex(mark) + ")";
            case 0b00: // locked
                return "locked(" + toHex(mark) + ")";
            case 0b01: // other
                String s = ", age=" + ((mark >> 3) & 0xF);
                int tribits = mark & 0b111;
                switch (tribits) {
                    case 0b001:
                        int hash = mark >>> 7;
                        if (hash != 0) {
                            return "hash=" + toHex(hash) + s;
                        } else {
                            return "neutral" + s;
                        }
                    case 0b101:
                        return "biased to thread " + toHex(mark >>> 9) + ", epoch=" + ((mark >> 7) & 0x2) + s;
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
                return "marked(" + toHex(mark) + ")";
            case 0b10: // has monitor
                return "monitor(" + toHex(mark) + ")";
            case 0b00: // locked
                return "locked(" + toHex(mark) + ")";
            case 0b01: // other
                String s = ", age=" + ((mark >> 3) & 0xF);
                int tribits = (int) (mark & 0b111);
                switch (tribits) {
                    case 0b001:
                        long hash = mark >>> 8;
                        if (hash != 0) {
                            return "hash=" + toHex(hash) + s;
                        } else {
                            return "neutral" + s;
                        }
                    case 0b101:
                        return "biased to thread " + toHex(mark >>> 10) + ", epoch=" + ((mark >> 8) & 0x2) + s;
                }
            default:
                return "(parse error)";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassLayout that = (ClassLayout) o;

        if (headerSize != that.headerSize) return false;
        if (size != that.size) return false;
        return fields.equals(that.fields);

    }

    @Override
    public int hashCode() {
        int result = fields.hashCode();
        result = 31 * result + headerSize;
        result = 31 * result + (int) (size ^ (size >>> 32));
        return result;
    }
}
