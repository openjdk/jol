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
package org.openjdk.jol.util;

import org.openjdk.jol.vm.VM;
import org.openjdk.jol.vm.VirtualMachine;

import java.lang.reflect.Field;
import java.util.Arrays;

public class ObjectUtils {

    /**
     * Produces the toString string, only calling toString() on known types,
     * which do not mutate the instance.
     *
     * @param o object to process
     * @return toString
     */
    public static String safeToString(Object o) {
        if (o == null) {
            return "null";
        }

        if (o.getClass().isArray()) {
            Class<?> type = o.getClass().getComponentType();
            if (type == boolean.class) {
                return Arrays.toString((boolean[]) o);
            }
            if (type == byte.class) {
                return Arrays.toString((byte[]) o);
            }
            if (type == short.class) {
                return Arrays.toString((short[]) o);
            }
            if (type == char.class) {
                return Arrays.toString((char[]) o);
            }
            if (type == int.class) {
                return Arrays.toString((int[]) o);
            }
            if (type == float.class) {
                return Arrays.toString((float[]) o);
            }
            if (type == long.class) {
                return Arrays.toString((long[]) o);
            }
            if (type == double.class) {
                return Arrays.toString((double[]) o);
            }

            Object[] oos = (Object[]) o;
            String[] strs = new String[oos.length];
            for (int i = 0; i < oos.length; i++) {
                strs[i] = (oos[i] == null) ? "null" : safeToString(oos[i]);
            }
            return Arrays.toString(strs);
        }

        if (o.getClass().isPrimitive()) {
            return o.toString();
        }
        if (o.getClass() == Boolean.class) {
            return o.toString();
        }
        if (o.getClass() == Byte.class) {
            return o.toString();
        }
        if (o.getClass() == Short.class) {
            return o.toString();
        }
        if (o.getClass() == Character.class) {
            return o.toString();
        }
        if (o.getClass() == Integer.class) {
            return o.toString();
        }
        if (o.getClass() == Float.class) {
            return o.toString();
        }
        if (o.getClass() == Long.class) {
            return o.toString();
        }
        if (o.getClass() == Double.class) {
            return o.toString();
        }
        return "(object)";
    }

    /**
     * Get the object field value.
     * @param o object to get field value from
     * @param f field descriptor
     * @return value, maybe a boxed primitive
     */
    public static Object value(Object o, Field f) {
        // Try 1. Get with Reflection:
        try {
            return f.get(o);
        } catch (Exception e) {
            // fall-through
        }

        // Try 2. Get with Reflection and setAccessible:
        try {
            f.setAccessible(true);
            return f.get(o);
        } catch (Exception e) {
            // fall-through
        }

        // Try 3. Get with VM hack
        VirtualMachine vm = VM.current();
        long off = vm.fieldOffset(f);
        Class<?> t = f.getType();
        if (t.isPrimitive()) {
            if (t == boolean.class) {
                return vm.getBoolean(o, off);
            } else
            if (t == byte.class) {
                return vm.getByte(o, off);
            } else
            if (t == char.class) {
                return vm.getChar(o, off);
            } else
            if (t == short.class) {
                return vm.getShort(o, off);
            } else
            if (t == int.class) {
                return vm.getInt(o, off);
            } else
            if (t == float.class) {
                return vm.getFloat(o, off);
            } else
            if (t == long.class) {
                return vm.getLong(o, off);
            } else
            if (t == double.class) {
                return vm.getDouble(o, off);
            } else {
                throw new IllegalStateException("Unhandled primitive: " + t);
            }
        } else {
            return vm.getObject(o, off);
        }
    }

}
