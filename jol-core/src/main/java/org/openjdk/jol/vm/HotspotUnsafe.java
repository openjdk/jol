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
import java.lang.reflect.Modifier;
import java.util.Random;

class HotspotUnsafe implements VirtualMachine {

    private final Unsafe U;
    private final Instrumentation instrumentation;

    private final boolean isAccurate;

    private final int     addressSize;
    private final int     objectAlignment;
    private final int     oopSize;
    private final boolean compressedOopsEnabled;
    private final long    narrowOopBase;
    private final int     narrowOopShift;
    private final int     klassOopSize;
    private final boolean compressedKlassOopsEnabled;
    private final long    narrowKlassBase;
    private final int     narrowKlassShift;

    private final int arrayHeaderSize;
    private final int objectHeaderSize;

    private final long arrayObjectBase;

    private final Sizes sizes;

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
        klassOopSize = saDetails.getKlassOopSize();

        objectHeaderSize = guessHeaderSize();
        arrayHeaderSize = objectHeaderSize + 4;

        compressedOopsEnabled = saDetails.isCompressedOopsEnabled();
        compressedKlassOopsEnabled = saDetails.isCompressedKlassOopsEnabled();

        objectAlignment = saDetails.getObjectAlignment();

        narrowOopShift = saDetails.getNarrowOopShift();
        narrowKlassShift = saDetails.getNarrowKlassShift();
        narrowOopBase = saDetails.getNarrowOopBase();
        narrowKlassBase = saDetails.getNarrowKlassBase();

        sizes = new Sizes(this);
    }

    HotspotUnsafe(Unsafe u, Instrumentation inst) {
        U = u;
        instrumentation = inst;
        isAccurate = false;

        arrayObjectBase = U.arrayBaseOffset(Object[].class);
        addressSize = U.addressSize();

        oopSize = guessOopSize();
        klassOopSize = oopSize;

        objectHeaderSize = guessHeaderSize();
        arrayHeaderSize = objectHeaderSize + 4;

        Boolean coops = VMOptions.pollCompressedOops();
        if (coops != null) {
            compressedOopsEnabled = coops;
            compressedKlassOopsEnabled = coops;
        } else {
            compressedOopsEnabled = (addressSize != oopSize);
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
    public String details() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        out.println("# Running " + (addressSize * 8) + "-bit HotSpot VM.");

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

        out.printf("# %-19s: %d, %d, %d, %d, %d, %d, %d, %d, %d [bytes]%n",
                "Field sizes by type",
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

        out.printf("# %-19s: %d, %d, %d, %d, %d, %d, %d, %d, %d [bytes]%n",
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
        if (Modifier.isStatic(field.getModifiers())) {
            return U.staticFieldOffset(field);
        } else {
            return U.objectFieldOffset(field);
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
