/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jol.vm;

import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.layouters.CurrentLayouter;
import org.openjdk.jol.util.MathUtil;
import org.openjdk.jol.vm.sa.UniverseData;
import sun.misc.Unsafe;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Random;

class HotspotUnsafe implements VirtualMachine {

    /*
        The reason why this option exists is public Unsafe.objectFieldOffset
        refusing to tell the field offset in Records, Hidden Classes and probably
        other future new Java object flavors:
            * https://bugs.openjdk.java.net/browse/JDK-8247444
            * https://github.com/openjdk/jdk/blob/de784312c340b4a4f4c4d11854bfbe9e9e826ea3/src/jdk.unsupported/share/classes/sun/misc/Unsafe.java#L644-L649

        Internal Unsafe still allows field offset access, but it requires several
        very dirty moves to access. Since the magic field offset code pokes (*writes*)
        into object internals, it is dangerous to use, and while the code tries to be
        as defensive as possible, it might still cause issues.
    */
    private static final String MAGIC_FIELD_OFFSET_OPTION = "jol.magicFieldOffset";

    private static final boolean MAGIC_FIELD_OFFSET =
            Boolean.parseBoolean(System.getProperty(MAGIC_FIELD_OFFSET_OPTION, "false"));

    private final Unsafe U;
    private final Instrumentation instrumentation;

    private final boolean isAccurate;

    private final int     addressSize;
    private final int     objectAlignment;
    private final int     oopSize;
    private final boolean compressedOopsEnabled;
    private final long    narrowOopBase;
    private final int     narrowOopShift;
    private final boolean compressedKlassOopsEnabled;
    private final long    narrowKlassBase;
    private final int     narrowKlassShift;

    private final int arrayHeaderSize;
    private final int objectHeaderSize;

    private final long arrayObjectBase;

    private final Sizes sizes;

    private final boolean lilliputVM;

    private volatile boolean mfoInitialized;
    private Object mfoUnsafe;
    private Method mfoMethod;

    private final ThreadLocal<Object[]> BUFFERS = new ThreadLocal<Object[]>() {
        @Override
        protected Object[] initialValue() {
            return new Object[1];
        }
    };


    HotspotUnsafe(Unsafe u, Instrumentation inst, UniverseData saDetails) {
        U = u;
        instrumentation = inst;
        isAccurate = true;

        arrayObjectBase = U.arrayBaseOffset(Object[].class);

        addressSize = saDetails.getAddressSize();
        oopSize = saDetails.getOopSize();

        objectHeaderSize = guessHeaderSize();
        arrayHeaderSize = objectHeaderSize + 4;

        compressedOopsEnabled = saDetails.isCompressedOopsEnabled();
        compressedKlassOopsEnabled = saDetails.isCompressedKlassPtrsEnabled();

        objectAlignment = saDetails.getObjectAlignment();

        narrowOopShift = saDetails.getNarrowOopShift();
        narrowKlassShift = saDetails.getNarrowKlassShift();
        narrowOopBase = saDetails.getNarrowOopBase();
        narrowKlassBase = saDetails.getNarrowKlassBase();

        sizes = new Sizes(this);
        lilliputVM = guessLilliput(objectHeaderSize);
    }

    HotspotUnsafe(Unsafe u, Instrumentation inst) {
        U = u;
        instrumentation = inst;
        isAccurate = false;

        arrayObjectBase = U.arrayBaseOffset(Object[].class);
        addressSize = U.addressSize();

        oopSize = guessOopSize();

        objectHeaderSize = guessHeaderSize();
        arrayHeaderSize = objectHeaderSize + 4;

        Boolean coops = VMOptions.pollCompressedOops();
        if (coops != null) {
            compressedOopsEnabled = coops;
        } else {
            compressedOopsEnabled = (addressSize != oopSize);
        }

        Boolean ccptrs = VMOptions.pollCompressedClassPointers();
        if (ccptrs != null) {
            compressedKlassOopsEnabled = ccptrs;
        } else {
            compressedKlassOopsEnabled = (addressSize != oopSize);
        }

        Integer align = VMOptions.pollObjectAlignment();
        if (align != null) {
            objectAlignment = align;
        } else {
            objectAlignment = guessAlignment();
        }

        if (compressedOopsEnabled) {
            narrowOopShift = MathUtil.log2p(objectAlignment);
        } else {
            narrowOopShift = 0;
        }

        if (compressedKlassOopsEnabled) {
            narrowKlassShift = MathUtil.log2p(objectAlignment);
        } else {
            narrowKlassShift = 0;
        }

        narrowOopBase = guessNarrowOopBase();
        narrowKlassBase = 0;

        sizes = new Sizes(this);

        lilliputVM = guessLilliput(addressSize);
    }

    private boolean guessLilliput(int addressSize) {
        // Lilliput encodes classes in mark word, so objects of different types
        // would be different there. Non Lilliput VMs can have different mark words
        // due to different prototype headers, so we try several times, with a little
        // (safepointing) sleep in between.
        for (int t = 0; t < 100; t++) {
            Object o1 = new Experiments.MyObject0();
            Object o2 = new Experiments.MyObject1();
            if (addressSize == 4) {
                if (getInt(o1, 0) == getInt(o2, 0)) {
                    // Mark words are identical, definitely not Lilliput.
                    return false;
                }
            } else if (addressSize == 8) {
                if (getLong(o1, 0) == getLong(o2, 0)) {
                    // Mark words are identical, definitely not Lilliput.
                    return false;
                }
            } else {
                throw new IllegalArgumentException("Unknown address size: " + addressSize);
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // Do nothing.
            }
        }
        return true;
    }

    private long guessNarrowOopBase() {
        return addressOf(null);
    }

    private int guessOopSize() {
        // When running with CompressedOops on 64-bit platform, the address size
        // reported by Unsafe is still 8, while the real reference fields are 4 bytes long.
        // Try to guess the reference field size with this naive trick.
        int oopSize;
        try {
            long off1 = U.objectFieldOffset(Experiments.CompressedOopsClass.class.getField("obj1"));
            long off2 = U.objectFieldOffset(Experiments.CompressedOopsClass.class.getField("obj2"));
            oopSize = (int) Math.abs(off2 - off1);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Infrastructure failure", e);
        }

        return oopSize;
    }

    private int guessHeaderSize() {
        try {
            long off1 = U.objectFieldOffset(Experiments.HeaderClass.class.getField("b1"));
            return (int) off1;
        } catch (NoSuchFieldException e) {
            return 0;
        }
    }

    @Override
    public long sizeOf(Object o) {
        if (instrumentation != null) {
            return MathUtil.align(instrumentation.getObjectSize(o), objectAlignment);
        }

        return new CurrentLayouter().layout(ClassData.parseInstance(o)).instanceSize();
    }

    @Override
    public long sizeOfField(String klassName) {
        return sizes.get(klassName);
    }

    @Override
    public int objectAlignment() {
        return objectAlignment;
    }

    @Override
    public int arrayHeaderSize() {
        return arrayHeaderSize;
    }

    @Override
    public int objectHeaderSize() {
        return objectHeaderSize;
    }

    private int getMinDiff(Class<?> klass) {
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

    @Override
    public int addressSize() {
        return addressSize;
    }

    @Override
    public int classPointerSize() {
        if (lilliputVM) {
            // Lilliput does not have a class word.
            return 0;
        }
        switch (addressSize) {
            case 4:
                return 4;
            case 8:
                return compressedKlassOopsEnabled ? 4 : 8;
            default:
                throw new IllegalStateException("Unknown address size:" + addressSize);
        }
    }

    @Override
    public String details() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        out.println("# Running " + (addressSize * 8) + "-bit HotSpot VM.");

        if (lilliputVM) {
            out.println("# Lilliput VM detected (experimental).");
        }

        if (compressedOopsEnabled) {
            if (narrowOopBase != 0) {
                out.println("# Using compressed oop with " +
                        formatAddressAsHexByAddressSize(narrowOopBase) + " base address and " +
                        narrowOopShift + "-bit shift.");
            } else {
                out.println("# Using compressed oop with " + narrowOopShift + "-bit shift.");
            }
        }
        if (compressedKlassOopsEnabled) {
            if (narrowKlassBase != 0) {
                out.println("# Using compressed klass with " +
                        formatAddressAsHexByAddressSize(narrowKlassBase) + " base address and " +
                        narrowKlassShift + "-bit shift.");
            } else {
                out.println("# Using compressed klass with " + narrowKlassShift + "-bit shift.");
            }
        }
        if (!isAccurate && (compressedOopsEnabled || compressedKlassOopsEnabled)) {
            out.println("# WARNING | Compressed references base/shifts are guessed by the experiment!");
            out.println("# WARNING | Therefore, computed addresses are just guesses, and ARE NOT RELIABLE.");
            out.println("# WARNING | Make sure to attach Serviceability Agent to get the reliable addresses.");
        }

        out.println("# Objects are " + objectAlignment + " bytes aligned.");

        out.printf("# %-20s %4s, %4s, %4s, %4s, %4s, %4s, %4s, %4s, %4s%n",
                "",
                "ref",
                "bool",
                "byte",
                "char",
                "shrt",
                "int",
                "flt",
                "lng",
                "dbl"
        );

        out.printf("# %-20s %4d, %4d, %4d, %4d, %4d, %4d, %4d, %4d, %4d%n",
                "Field sizes:",
                oopSize,
                sizes.booleanSize,
                sizes.byteSize,
                sizes.charSize,
                sizes.shortSize,
                sizes.intSize,
                sizes.floatSize,
                sizes.longSize,
                sizes.doubleSize
        );

        out.printf("# %-20s %4d, %4d, %4d, %4d, %4d, %4d, %4d, %4d, %4d%n",
                "Array element sizes:",
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

        out.printf("# %-20s %4d, %4d, %4d, %4d, %4d, %4d, %4d, %4d, %4d%n",
                "Array base offsets:",
                U.arrayBaseOffset(Object[].class),
                U.arrayBaseOffset(boolean[].class),
                U.arrayBaseOffset(byte[].class),
                U.arrayBaseOffset(char[].class),
                U.arrayBaseOffset(short[].class),
                U.arrayBaseOffset(int[].class),
                U.arrayBaseOffset(float[].class),
                U.arrayBaseOffset(long[].class),
                U.arrayBaseOffset(double[].class)
        );

        out.close();
        return sw.toString();
    }

    private static Object instantiateType(int type) {
        switch (type) {
            case 0: return new Experiments.MyObject1();
            case 1: return new Experiments.MyObject2();
            case 2: return new Experiments.MyObject3();
            case 3: return new Experiments.MyObject4();
            case 4: return new Experiments.MyObject5();
            default:
                throw new IllegalStateException();
        }
    }

    private int guessAlignment() {
        final int COUNT = 100000;

        Random r = new Random();

        long min = -1;
        for (int c = 0; c < COUNT; c++) {
            Object o1 = instantiateType(r.nextInt(5));
            Object o2 = instantiateType(r.nextInt(5));
            long diff = Math.abs(addressOf(o1) - addressOf(o2));
            if (min == -1) {
                min = diff;
            } else {
                min = MathUtil.gcd(min, diff);
            }
        }

        return (int) min;
    }

    @Override
    public long addressOf(Object o) {
        Object[] array = BUFFERS.get();

        array[0] = o;

        long objectAddress;
        switch (oopSize) {
            case 4:
                objectAddress = U.getInt(array, arrayObjectBase) & 0xFFFFFFFFL;
                break;
            case 8:
                objectAddress = U.getLong(array, arrayObjectBase);
                break;
            default:
                throw new Error("unsupported address size: " + oopSize);
        }

        array[0] = null;

        return toNativeAddress(objectAddress);
    }


    @Override
    public int arrayBaseOffset(String arrayComponentKlass) {
        if (arrayComponentKlass.equals("byte"))    return U.arrayBaseOffset(byte[].class);
        if (arrayComponentKlass.equals("boolean")) return U.arrayBaseOffset(boolean[].class);
        if (arrayComponentKlass.equals("short"))   return U.arrayBaseOffset(short[].class);
        if (arrayComponentKlass.equals("char"))    return U.arrayBaseOffset(char[].class);
        if (arrayComponentKlass.equals("int"))     return U.arrayBaseOffset(int[].class);
        if (arrayComponentKlass.equals("float"))   return U.arrayBaseOffset(float[].class);
        if (arrayComponentKlass.equals("long"))    return U.arrayBaseOffset(long[].class);
        if (arrayComponentKlass.equals("double"))  return U.arrayBaseOffset(double[].class);
        return U.arrayBaseOffset(Object[].class);
    }

    @Override
    public int arrayIndexScale(String arrayComponentKlass) {
        if (arrayComponentKlass.equals("byte"))    return U.arrayIndexScale(byte[].class);
        if (arrayComponentKlass.equals("boolean")) return U.arrayIndexScale(boolean[].class);
        if (arrayComponentKlass.equals("short"))   return U.arrayIndexScale(short[].class);
        if (arrayComponentKlass.equals("char"))    return U.arrayIndexScale(char[].class);
        if (arrayComponentKlass.equals("int"))     return U.arrayIndexScale(int[].class);
        if (arrayComponentKlass.equals("float"))   return U.arrayIndexScale(float[].class);
        if (arrayComponentKlass.equals("long"))    return U.arrayIndexScale(long[].class);
        if (arrayComponentKlass.equals("double"))  return U.arrayIndexScale(double[].class);
        return U.arrayIndexScale(Object[].class);
    }

    @Override
    public boolean getBoolean(Object obj, long offset) {
        return U.getBoolean(obj, offset);
    }

    @Override
    public byte getByte(Object obj, long offset) {
        return U.getByte(obj, offset);
    }

    @Override
    public short getShort(Object obj, long offset) {
        return U.getShort(obj, offset);
    }

    @Override
    public char getChar(Object obj, long offset) {
        return U.getChar(obj, offset);
    }

    @Override
    public int getInt(Object obj, long offset) {
        return U.getInt(obj, offset);
    }

    @Override
    public float getFloat(Object obj, long offset) {
        return U.getFloat(obj, offset);
    }

    @Override
    public long getLong(Object obj, long offset) {
        return U.getLong(obj, offset);
    }

    @Override
    public double getDouble(Object obj, long offset) {
        return U.getDouble(obj, offset);
    }

    @Override
    public Object getObject(Object obj, long offset) {
        return U.getObject(obj, offset);
    }

    @Override
    public long fieldOffset(Field field) {
        try {
            if (Modifier.isStatic(field.getModifiers())) {
                return U.staticFieldOffset(field);
            } else {
                return U.objectFieldOffset(field);
            }
        } catch (UnsupportedOperationException uoe) {
            if (MAGIC_FIELD_OFFSET) {
                // Access denied? Try again with magic method.
                return magicFieldOffset(field, uoe);
            } else {
                throw new RuntimeException("Cannot get the field offset, try with -D" + MAGIC_FIELD_OFFSET_OPTION + "=true", uoe);
            }
        }
    }

    private long magicFieldOffset(Field field, RuntimeException original) {
        if (!mfoInitialized) {
            // YOLO Engineering, part (N+1):
            // Guess where the magic boolean field is in AccessibleObject.
            long magicOffset = -1;
            try {
                // Candidates for testing: one with setAccessible "true", another with "false".
                // The experiment would try to figure out what object field offset differ.
                // Note that setAccessible should always work here, since we are reflecting
                // to ourselves.
                Method acFalse = HotspotUnsafe.class.getDeclaredMethod("magicFieldOffset",
                        Field.class, RuntimeException.class);
                Method acTrue = HotspotUnsafe.class.getDeclaredMethod("magicFieldOffset",
                        Field.class, RuntimeException.class);
                acFalse.setAccessible(false);
                acTrue.setAccessible(true);

                // Victim candidate to juggle the setAccessible back and forth for more testing.
                Method acTest = HotspotUnsafe.class.getDeclaredMethod("magicFieldOffset",
                        Field.class, RuntimeException.class);

                // Try to find the last plausible offset.
                long sizeOf = sizeOf(acFalse);
                for (long off = sizeOf - 1; off >= 0; off--) {
                    boolean vFalse = U.getBoolean(acFalse, off);
                    boolean vTrue  = U.getBoolean(acTrue, off);
                    if (!vFalse && vTrue) {
                        // Potential candidate offset. Verify that every transition
                        // reflects the change in observed value: ? -> T, T -> F, F -> T.
                        boolean good = true;
                        for (int t = 0; t < 3; t++) {
                            boolean test = (t & 0x1) == 0;
                            acTest.setAccessible(test);
                            if (U.getBoolean(acTest, off) != test) {
                                good = false;
                                break;
                            }
                        }
                        if (good) {
                            // The confidence is HIGH. Remember it.
                            magicOffset = off;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // Do nothing.
            }

            if (magicOffset != -1) {
                try {
                    // Figure out the way to internal Unsafe
                    Class<?> cl = Class.forName("jdk.internal.misc.Unsafe");
                    Field fu = cl.getDeclaredField("theUnsafe");
                    mfoUnsafe = U.getObject(U.staticFieldBase(fu), U.staticFieldOffset(fu));

                    // Figure out the magic accessor
                    Method mfo = mfoUnsafe.getClass().getMethod("objectFieldOffset", Field.class);

                    // Check if magic accessor is accessible.
                    Method canAccess = Method.class.getMethod("canAccess", Object.class);
                    if (!(boolean)canAccess.invoke(mfo, mfoUnsafe)) {
                        // Not accessible. Nothing we can do, except this last-ditch...
                        // DIRTY HACK: Side-step module protections by overriding the access
                        // control check. This allows calling internal Unsafe methods that is
                        // normally disallowed.

                        // Save the old int-aligned slot for the fallback
                        long slotOffset = (magicOffset >> 2) << 2;
                        int old = U.getInt(mfo, slotOffset);

                        // NAKED MEMORY STORE. Here be dragons.
                        U.putBoolean(mfo, magicOffset, true);

                        // Check that we succeeded?
                        if (!(boolean)canAccess.invoke(mfo, mfoUnsafe)) {
                            // Failed! Put the old value back in.
                            U.putInt(mfo, slotOffset, old);
                            throw new IllegalStateException("Hard failure: magic offset calculation must be wrong");
                        }
                    }

                    // All good? Install the method as resolved.
                    mfoMethod = mfo;
                } catch (Exception e) {
                    // Do nothing.
                }
            }

            mfoInitialized = true;
        }

        if (mfoMethod == null || mfoUnsafe == null) {
            throw original;
        }

        try {
            return (long) mfoMethod.invoke(mfoUnsafe, field);
        } catch (Exception e) {
            RuntimeException ex = new IllegalStateException("Unable to get the offset for " + field);
            ex.addSuppressed(original);
            ex.addSuppressed(e);
            throw ex;
        }
    }

    private long toNativeAddress(long address) {
        if (compressedOopsEnabled) {
            return narrowOopBase + (address << narrowOopShift);
        } else {
            return address;
        }
    }

    private long toJvmAddress(long address) {
        if (compressedOopsEnabled) {
            return (address >> narrowOopShift) - narrowOopBase;
        } else {
            return address;
        }
    }

    private long toNativeOopAddress(long address) {
        if (compressedOopsEnabled) {
            return narrowOopBase + (address << narrowOopShift);
        } else {
            return address;
        }
    }

    private long toJvmOopAddress(long address) {
        if (compressedOopsEnabled) {
            return (address >> narrowOopShift) - narrowOopBase;
        } else {
            return address;
        }
    }

    private long toNativeKlassAddress(long address) {
        if (compressedKlassOopsEnabled) {
            return narrowKlassBase + (address << narrowKlassShift);
        } else {
            return address;
        }
    }

    private long toJvmKlassAddress(long address) {
        if (compressedKlassOopsEnabled) {
            return (address >> narrowKlassShift) - narrowKlassBase;
        } else {
            return address;
        }
    }

    private String formatAddressAsHexByAddressSize(long address) {
        return "0x" + String.format("%" + (addressSize * 2) + "s",
                Long.toHexString(address).toUpperCase()).replace(' ', '0');
    }

    private static class Sizes {
        private final int booleanSize;
        private final int byteSize;
        private final int shortSize;
        private final int charSize;
        private final int floatSize;
        private final int intSize;
        private final int longSize;
        private final int doubleSize;
        private final int oopSize;

        Sizes(HotspotUnsafe vm) {
            booleanSize = vm.getMinDiff(Experiments.MyBooleans4.class);
            byteSize = vm.getMinDiff(Experiments.MyBytes4.class);
            shortSize = vm.getMinDiff(Experiments.MyShorts4.class);
            charSize = vm.getMinDiff(Experiments.MyChars4.class);
            floatSize = vm.getMinDiff(Experiments.MyFloats4.class);
            intSize = vm.getMinDiff(Experiments.MyInts4.class);
            longSize = vm.getMinDiff(Experiments.MyLongs4.class);
            doubleSize = vm.getMinDiff(Experiments.MyDoubles4.class);
            oopSize = vm.oopSize;
        }

        public long get(String klassName) {
            if (klassName.equals("byte"))    return byteSize;
            if (klassName.equals("boolean")) return booleanSize;
            if (klassName.equals("short"))   return shortSize;
            if (klassName.equals("char"))    return charSize;
            if (klassName.equals("int"))     return intSize;
            if (klassName.equals("float"))   return floatSize;
            if (klassName.equals("long"))    return longSize;
            if (klassName.equals("double"))  return doubleSize;
            return oopSize;
        }
    }

}
