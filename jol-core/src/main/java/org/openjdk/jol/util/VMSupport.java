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
package org.openjdk.jol.util;

import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.CurrentLayouter;
import org.openjdk.jol.util.sa.impl.compressedrefs.HS_SA_CompressedReferencesResult;

import sun.misc.Unsafe;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeDataSupport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VM support doorway.
 * Contains all the special tricks and methods to poll VM about it's secrets.
 *
 * @author Aleksey Shipilev
 */
public class VMSupport {

    public static final Unsafe U;

    public static final String VM_NAME;
    public static final int ADDRESS_SIZE;
    public static final int REF_SIZE;
    public static final int OBJ_ALIGNMENT;
    public static final int OBJ_HEADER_SIZE;
    public static final int ARRAY_HEADER_SIZE;

    public static final boolean USE_COMPRESSED_REFS;
    public static final long COMPRESSED_REF_BASE;
    public static final int COMPRESSED_REF_SHIFT;

    public static final int OOP_SIZE;
    public static final boolean USE_COMPRESSED_OOP;
    public static final long COMPRESSED_OOP_BASE;
    public static final int COMPRESSED_OOP_SHIFT;

    public static final int KLASS_OOP_SIZE;
    public static final boolean USE_COMPRESSED_KLASS;
    public static final long COMPRESSED_KLASS_BASE;
    public static final int COMPRESSED_KLASS_SHIFT;

    public static final int BOOLEAN_SIZE;
    public static final int BYTE_SIZE;
    public static final int CHAR_SIZE;
    public static final int DOUBLE_SIZE;
    public static final int FLOAT_SIZE;
    public static final int INT_SIZE;
    public static final int LONG_SIZE;
    public static final int SHORT_SIZE;

    private static final ThreadLocal<Object[]> BUFFERS;
    private static final long OBJECT_ARRAY_BASE;

    static {
        U = AccessController.doPrivileged(
                new PrivilegedAction<Unsafe>() {
                    public Unsafe run() {
                        try {
                            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
                            unsafe.setAccessible(true);
                            return (Unsafe) unsafe.get(null);
                        } catch (NoSuchFieldException e) {
                            throw new IllegalStateException(e);
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
        );

        OBJECT_ARRAY_BASE = U.arrayBaseOffset(Object[].class);
        BUFFERS = new ThreadLocal<Object[]>() {
            @Override
            protected Object[] initialValue() {
                return new Object[1];
            }
        };

        int headerSize;
        try {
            long off1 = U.objectFieldOffset(HeaderClass.class.getField("b1"));
            headerSize = (int) off1;
        } catch (NoSuchFieldException e) {
            headerSize = -1;
        }

        ADDRESS_SIZE = U.addressSize();

        VMOptions opts = VMOptions.getOptions();

        VM_NAME = opts.name;
        REF_SIZE = opts.sizeReference;
        OBJ_ALIGNMENT = opts.objectAlignment;
        OBJ_HEADER_SIZE = headerSize;
        ARRAY_HEADER_SIZE = OBJ_HEADER_SIZE + 4;

        /*
         * There are two different compressed references (OOP and Klass) information since Java 8.
         * For Java 6 and 7, there is only one (OOP) compressed reference and this one is used both of OOP and Klass instances.
         * So we can assume compressed-oop information as compressed-reference information
         * for "USE_COMPRESSED_REFS", COMPRESSED_REF_BASE" and "COMPRESSED_REF_SHIFT" properties as default.
         */

        USE_COMPRESSED_REFS = opts.compressedOopRef;
        COMPRESSED_REF_BASE = opts.compressedOopBase;
        COMPRESSED_REF_SHIFT = opts.compressedOopShift;

        OOP_SIZE = opts.oopSize;
        USE_COMPRESSED_OOP = opts.compressedOopRef;
        COMPRESSED_OOP_BASE = opts.compressedOopBase;
        COMPRESSED_OOP_SHIFT = opts.compressedOopShift;

        KLASS_OOP_SIZE = opts.klassOopSize;
        USE_COMPRESSED_KLASS = opts.compressedKlassRef;
        COMPRESSED_KLASS_BASE = opts.compressedKlassBase;
        COMPRESSED_KLASS_SHIFT = opts.compressedKlassShift;

        BOOLEAN_SIZE = opts.sizeBoolean;
        BYTE_SIZE = opts.sizeByte;
        CHAR_SIZE = opts.sizeChar;
        DOUBLE_SIZE = opts.sizeDouble;
        FLOAT_SIZE = opts.sizeFloat;
        INT_SIZE = opts.sizeInt;
        LONG_SIZE = opts.sizeLong;
        SHORT_SIZE = opts.sizeShort;
    }

    public static long toNativeAddress(long address) {
        if (USE_COMPRESSED_REFS) {
            return COMPRESSED_REF_BASE + (address << COMPRESSED_REF_SHIFT);
        } else {
            return address;
        }
    }

    public static long toJvmAddress(long address) {
        if (USE_COMPRESSED_REFS) {
            return (address >> COMPRESSED_REF_SHIFT) - COMPRESSED_REF_BASE;
        } else {
            return address;
        }
    }

    public static long toNativeOopAddress(long address) {
        if (USE_COMPRESSED_OOP) {
            return COMPRESSED_OOP_BASE + (address << COMPRESSED_OOP_SHIFT);
        } else {
            return address;
        }
    }

    public static long toJvmOopAddress(long address) {
        if (USE_COMPRESSED_OOP) {
            return (address >> COMPRESSED_OOP_SHIFT) - COMPRESSED_OOP_BASE;
        } else {
            return address;
        }
    }

    public static long toNativeKlassAddress(long address) {
        if (USE_COMPRESSED_KLASS) {
            return COMPRESSED_KLASS_BASE + (address << COMPRESSED_KLASS_SHIFT);
        } else {
            return address;
        }
    }

    public static long toJvmKlassAddress(long address) {
        if (USE_COMPRESSED_KLASS) {
            return (address >> COMPRESSED_KLASS_SHIFT) - COMPRESSED_KLASS_BASE;
        } else {
            return address;
        }
    }

    public static int align(int addr) {
        return MathUtil.align(addr, OBJ_ALIGNMENT);
    }

    public static String vmDetails() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        out.println("Running " + (ADDRESS_SIZE * 8) + "-bit " + VM_NAME + " VM.");

        String javaSpecVersion = System.getProperty("java.specification.version");
        // Since Java 8 (Java 8 and Java 9) has different compressed reference configuration for OOP and Klass
        if (javaSpecVersion.equals("1.8") || javaSpecVersion.equals("1.9")) {
            if (USE_COMPRESSED_OOP) {
                if (COMPRESSED_OOP_BASE != 0) {
                    out.println("Using compressed oop with " +
                                formatAddressAsHexByAddressSize(COMPRESSED_OOP_BASE) + " base address and " +
                                COMPRESSED_OOP_SHIFT + "-bit shift.");
                } else {
                    out.println("Using compressed oop with " + COMPRESSED_OOP_SHIFT + "-bit shift.");
                }
            }
            if (USE_COMPRESSED_KLASS) {
                if (COMPRESSED_KLASS_BASE != 0) {
                    out.println("Using compressed klass with " +
                                formatAddressAsHexByAddressSize(COMPRESSED_KLASS_BASE) + " base address and " +
                                COMPRESSED_KLASS_SHIFT + "-bit shift.");
                } else {
                    out.println("Using compressed klass with " + COMPRESSED_KLASS_SHIFT + "-bit shift.");
                }
            }
        } else {
            if (USE_COMPRESSED_REFS) {
                if (COMPRESSED_REF_BASE != 0) {
                    out.println("Using compressed references with " +
                            formatAddressAsHexByAddressSize(COMPRESSED_REF_BASE) + " base address and " +
                            COMPRESSED_REF_SHIFT + "-bit shift.");
                } else {
                    out.println("Using compressed references with " + COMPRESSED_REF_SHIFT + "-bit shift.");
                }
            }
        }
        out.println("Objects are " + OBJ_ALIGNMENT + " bytes aligned.");

        out.printf("%-19s: %d, %d, %d, %d, %d, %d, %d, %d, %d [bytes]%n",
                "Field sizes by type",
                REF_SIZE,
                BOOLEAN_SIZE,
                BYTE_SIZE,
                CHAR_SIZE,
                SHORT_SIZE,
                INT_SIZE,
                FLOAT_SIZE,
                LONG_SIZE,
                DOUBLE_SIZE
        );

        out.printf("%-19s: %d, %d, %d, %d, %d, %d, %d, %d, %d [bytes]%n",
                "Array element sizes",
                U.arrayIndexScale(Object[].class),
                U.arrayIndexScale(boolean[].class),
                U.arrayIndexScale(byte[].class),
                U.arrayIndexScale(char[].class),
                U.arrayIndexScale(short[].class),
                U.arrayIndexScale(int[].class),
                U.arrayIndexScale(float[].class),
                U.arrayIndexScale(long[].class),
                U.arrayIndexScale(double[].class)
        );

        out.close();
        return sw.toString();
    }

    private static String formatAddressAsHexByAddressSize(long address) {
        return "0x" + String.format("%" + (ADDRESS_SIZE * 2) + "s",
                                    Long.toHexString(address).toUpperCase()).replace(' ', '0');
    }

    private static Object instantiateType(int type) {
        switch (type) {
            case 0: return new MyObject1();
            case 1: return new MyObject2();
            case 2: return new MyObject3();
            case 3: return new MyObject4();
            case 4: return new MyObject5();
            default:
                throw new IllegalStateException();
        }
    }

    private static int guessAlignment(int oopSize) {
        final int COUNT = 100000;

        Random r = new Random();

        long min = -1;
        for (int c = 0; c < COUNT; c++) {
            Object o1 = instantiateType(r.nextInt(5));
            Object o2 = instantiateType(r.nextInt(5));

            long diff = Math.abs(addressOf(o2, oopSize) - addressOf(o1, oopSize));
            if (min == -1) {
                min = diff;
            } else {
                min = MathUtil.gcd(min, diff);
            }
        }

        return (int) min;
    }

    public static long addressOf(Object o) {
        return addressOf(o, REF_SIZE);
    }

    public static long addressOf(Object o, int oopSize) {
        Object[] array = BUFFERS.get();

        array[0] = o;

        long objectAddress;
        switch (oopSize) {
            case 4:
                objectAddress = U.getInt(array, OBJECT_ARRAY_BASE) & 0xFFFFFFFFL;
                break;
            case 8:
                objectAddress = U.getLong(array, OBJECT_ARRAY_BASE);
                break;
            default:
                throw new Error("unsupported address size: " + oopSize);
        }

        array[0] = null;

        return toNativeAddress(objectAddress);
    }

    public static SizeInfo tryExactObjectSize(Object o, ClassLayout layout) {
        return new SizeInfo(o, layout);
    }

    public static class SizeInfo {
        private final int size;
        private final boolean exactSizeAvail;

        public SizeInfo(Object o, ClassLayout layout) {
            exactSizeAvail = InstrumentationSupport.instance() != null && o != null;
            size = exactSizeAvail ? (int) InstrumentationSupport.instance().getObjectSize(o) : layout.instanceSize();
        }

        public int instanceSize() {
            return size;
        }

        public boolean exactSize() {
            return exactSizeAvail;
        }
    }

    private static class VMOptions {
        private final String name;
        private final int objectAlignment;
        private final int oopSize;
        private final boolean compressedOopRef;
        private final long compressedOopBase;
        private final int compressedOopShift;
        private final int klassOopSize;
        private final boolean compressedKlassRef;
        private final long compressedKlassBase;
        private final int compressedKlassShift;

        private final int sizeReference;
        private final int sizeBoolean = getMinDiff(MyBooleans4.class);
        private final int sizeByte = getMinDiff(MyBytes4.class);
        private final int sizeShort = getMinDiff(MyShorts4.class);
        private final int sizeChar = getMinDiff(MyChars4.class);
        private final int sizeFloat = getMinDiff(MyFloats4.class);
        private final int sizeInt = getMinDiff(MyInts4.class);
        private final int sizeLong = getMinDiff(MyLongs4.class);
        private final int sizeDouble = getMinDiff(MyDoubles4.class);

        public static int getMinDiff(Class<?> klass) {
            try {
                int off1 = (int) U.objectFieldOffset(klass.getDeclaredField("f1"));
                int off2 = (int) U.objectFieldOffset(klass.getDeclaredField("f2"));
                int off3 = (int) U.objectFieldOffset(klass.getDeclaredField("f3"));
                int off4 = (int) U.objectFieldOffset(klass.getDeclaredField("f4"));
                return MathUtil.minDiff(off1, off2, off3, off4);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Infrastructure failure, klass = " + klass, e);
            }
        }

        public VMOptions(String name) {
            this.name = name;
            this.sizeReference = U.addressSize();
            this.objectAlignment = guessAlignment(this.sizeReference);
            this.oopSize = sizeReference;
            this.compressedOopRef = false;
            this.compressedOopBase = 0L;
            this.compressedOopShift = 0;
            this.klassOopSize = sizeReference;
            this.compressedKlassRef = false;
            this.compressedKlassBase = 0L;
            this.compressedKlassShift = 0;
        }

        public VMOptions(String name, int align) {
            this.name = name;
            this.sizeReference = 4;
            this.objectAlignment = align;
            this.oopSize = sizeReference;
            this.compressedOopRef = true;
            this.compressedOopBase = 0L;
            this.compressedOopShift = MathUtil.log2p(align);
            this.klassOopSize = sizeReference;
            this.compressedKlassRef = true;
            this.compressedKlassBase = 0L;
            this.compressedKlassShift = MathUtil.log2p(align);
        }

        public VMOptions(String name, int align, int compRefShift) {
            this.name = name;
            this.sizeReference = 4;
            this.objectAlignment = align;
            this.oopSize = sizeReference;
            this.compressedOopRef = true;
            this.compressedOopBase = 0L;
            this.compressedOopShift = compRefShift;
            this.klassOopSize = sizeReference;
            this.compressedKlassRef = true;
            this.compressedKlassBase = 0L;
            this.compressedKlassShift = compRefShift;
        }

        public VMOptions(String name, int align, int oopSize, boolean compOopRef, long compOopBase, int compOopShift,
                int klassOopSize, boolean compKlassRef, long compKlassBase, int compKlassShift) {
            this.name = name;
            // Use OOP size as reference size
            this.sizeReference = oopSize;
            this.objectAlignment = align;
            this.oopSize = oopSize;
            this.compressedOopRef = compOopRef;
            this.compressedOopBase = compOopBase;
            this.compressedOopShift = compOopShift;
            this.klassOopSize = klassOopSize;
            this.compressedKlassRef = compKlassRef;
            this.compressedKlassBase = compKlassBase;
            this.compressedKlassShift = compKlassShift;
        }

        private static VMOptions getOptions() {
            // try Hotspot
            VMOptions hsOpts = getHotspotSpecifics();
            if (hsOpts != null) return hsOpts;

            // try JRockit
            VMOptions jrOpts = getJRockitSpecifics();
            if (jrOpts != null) return jrOpts;

            // When running with CompressedOops on 64-bit platform, the address size
            // reported by Unsafe is still 8, while the real reference fields are 4 bytes long.
            // Try to guess the reference field size with this naive trick.
            int oopSize;
            try {
                long off1 = U.objectFieldOffset(CompressedOopsClass.class.getField("obj1"));
                long off2 = U.objectFieldOffset(CompressedOopsClass.class.getField("obj2"));
                oopSize = (int) Math.abs(off2 - off1);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Infrastructure failure", e);
            }

            if (oopSize != U.addressSize()) {
                return new VMOptions("Auto-detected", 3); // assume compressed references have << 3 shift
            } else {
                return new VMOptions("Auto-detected");
            }
        }

        private static VMOptions getHotspotSpecifics() {
            String name = System.getProperty("java.vm.name");
            if (!name.contains("HotSpot") && !name.contains("OpenJDK")) {
                return null;
            }

            try {
                try {
                    HS_SA_CompressedReferencesResult compressedReferencesInfo =
                            HS_SA_Support.getCompressedReferences();
                    if (compressedReferencesInfo != null) {
                        return new VMOptions("HotSpot",
                                             compressedReferencesInfo.getObjectAlignment(),
                                             compressedReferencesInfo.getOopSize(),
                                             compressedReferencesInfo.isCompressedOopsEnabled(),
                                             compressedReferencesInfo.getNarrowOopBase(),
                                             compressedReferencesInfo.getNarrowOopShift(),
                                             compressedReferencesInfo.getKlassOopSize(),
                                             compressedReferencesInfo.isCompressedKlassOopsEnabled(),
                                             compressedReferencesInfo.getNarrowKlassBase(),
                                             compressedReferencesInfo.getNarrowKlassShift());
                    } else {
                        System.out.println("Compressed references information couldn't be found via Hotspot SA.");
                    }
                } catch (Throwable t) {
                    System.err.println(t.getMessage());
                    System.err.println("WARNING: VM details, e.g. object alignment, reference size, compressed references info will be guessed.");
                    System.err.println();
                }

                MBeanServer server = ManagementFactory.getPlatformMBeanServer();

                try {
                    ObjectName mbean = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
                    CompositeDataSupport compressedOopsValue = (CompositeDataSupport) server.invoke(mbean, "getVMOption", new Object[]{"UseCompressedOops"}, new String[]{"java.lang.String"});
                    boolean compressedOops = Boolean.valueOf(compressedOopsValue.get("value").toString());
                    if (compressedOops) {
                        // if compressed oops are enabled, then this option is also accessible
                        CompositeDataSupport alignmentValue = (CompositeDataSupport) server.invoke(mbean, "getVMOption", new Object[]{"ObjectAlignmentInBytes"}, new String[]{"java.lang.String"});
                        int align = Integer.valueOf(alignmentValue.get("value").toString());
                        return new VMOptions("HotSpot", align);
                    } else {
                        return new VMOptions("HotSpot");
                    }

                } catch (RuntimeMBeanException iae) {
                    return new VMOptions("HotSpot");
                }
            } catch (RuntimeException re) {
                System.err.println("Failed to read HotSpot-specific configuration properly, please report this as the bug");
                re.printStackTrace();
                return null;
            } catch (Exception exp) {
                System.err.println("Failed to read HotSpot-specific configuration properly, please report this as the bug");
                exp.printStackTrace();
                return null;
            }
        }

        private static VMOptions getJRockitSpecifics() {
            String name = System.getProperty("java.vm.name");
            if (!name.contains("JRockit")) {
                return null;
            }

            try {
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                String str = (String) server.invoke(new ObjectName("oracle.jrockit.management:type=DiagnosticCommand"), "execute", new Object[]{"print_vm_state"}, new String[]{"java.lang.String"});
                String[] split = str.split("\n");
                for (String s : split) {
                    if (s.contains("CompRefs")) {
                        Pattern pattern = Pattern.compile("(.*?)References are compressed, with heap base (.*?) and shift (.*?)\\.");
                        Matcher matcher = pattern.matcher(s);
                        if (matcher.matches()) {
                            return new VMOptions("JRockit (experimental)", 8, Integer.valueOf(matcher.group(3)));
                        } else {
                            return new VMOptions("JRockit (experimental)");
                        }
                    }
                }
                return null;
            } catch (RuntimeException re) {
                System.err.println("Failed to read JRockit-specific configuration properly, please report this as the bug");
                re.printStackTrace();
                return null;
            } catch (Exception exp) {
                System.err.println("Failed to read JRockit-specific configuration properly, please report this as the bug");
                exp.printStackTrace();
                return null;
            }
        }

    }

    public static int sizeOf(Object o) {
        if (InstrumentationSupport.instance() != null) {
            return VMSupport.align((int) InstrumentationSupport.instance().getObjectSize(o));
        }

        return new CurrentLayouter().layout(ClassData.parseInstance(o)).instanceSize();
    }

    /**
     * Produces the toString string, only calling toString() on known types,
     * which do not mutate the instance.
     *
     * @param o object to process
     * @return toString
     */
    public static String safeToString(Object o) {
        if (o == null) return "null";

        if (o.getClass().isArray()) {
            Class<?> type = o.getClass().getComponentType();
            if (type == boolean.class) return Arrays.toString((boolean[]) o);
            if (type == byte.class) return Arrays.toString((byte[]) o);
            if (type == short.class) return Arrays.toString((short[]) o);
            if (type == char.class) return Arrays.toString((char[]) o);
            if (type == int.class) return Arrays.toString((int[]) o);
            if (type == float.class) return Arrays.toString((float[]) o);
            if (type == long.class) return Arrays.toString((long[]) o);
            if (type == double.class) return Arrays.toString((double[]) o);

            Object[] oos = (Object[]) o;
            String[] strs = new String[oos.length];
            for (int i = 0; i < oos.length; i++) {
                strs[i] = (oos[i] == null) ? "null" : safeToString(oos[i]);
            }
            return Arrays.toString(strs);
        }

        if (o.getClass().isPrimitive()) return o.toString();
        if (o.getClass() == Boolean.class) return o.toString();
        if (o.getClass() == Byte.class) return o.toString();
        if (o.getClass() == Short.class) return o.toString();
        if (o.getClass() == Character.class) return o.toString();
        if (o.getClass() == Integer.class) return o.toString();
        if (o.getClass() == Float.class) return o.toString();
        if (o.getClass() == Long.class) return o.toString();
        if (o.getClass() == Double.class) return o.toString();
        return "(object)";
    }


    static class CompressedOopsClass {
        public Object obj1;
        public Object obj2;
    }

    static class HeaderClass {
        public boolean b1;
    }

    static class MyObject1 {

    }

    static class MyObject2 {
        private boolean b;
    }

    static class MyObject3 {
        private int i;
    }

    static class MyObject4 {
        private long l;
    }

    static class MyObject5 {
        private Object o;
    }

    static class MyBooleans4 {
        private boolean f1, f2, f3, f4;
    }

    static class MyBytes4 {
        private byte f1, f2, f3, f4;
    }

    static class MyShorts4 {
        private short f1, f2, f3, f4;
    }

    static class MyChars4 {
        private char f1, f2, f3, f4;
    }

    static class MyInts4 {
        private int f1, f2, f3, f4;
    }

    static class MyFloats4 {
        private float f1, f2, f3, f4;
    }

    static class MyLongs4 {
        private long f1, f2, f3, f4;
    }

    static class MyDoubles4 {
        private double f1, f2, f3, f4;
    }

}
