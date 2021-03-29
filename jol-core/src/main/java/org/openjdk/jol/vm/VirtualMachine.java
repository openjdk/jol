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

import java.lang.reflect.Field;

public interface VirtualMachine {

    /**
     * Returns the shallow size of the given object.
     * @param obj object
     * @return shallow size
     */
    long sizeOf(Object obj);

    /**
     * Returns the size of a field holding the type.
     * @param klass klass
     * @return slot size
     */
    long sizeOfField(String klass);

    /**
     * Returns the machine address of the given object.
     * Note that in some VM modes, the addresses would be guesses, based on
     * internal experiments which would try to figure out the reference encoding.
     * Use this data with care. Doing the naked memory access on the result of
     * this method may corrupt the memory.
     *
     * @param obj object
     * @return address
     */
    long addressOf(Object obj);

    /**
     * Returns the field offset for a given field, starting from the object base.
     * @param field field
     * @return offset
     */
    long fieldOffset(Field field);

    /**
     * Returns the array base offset for an array of a given component type.
     * @param arrayComponentKlass component type
     * @return base offset
     */
    int arrayBaseOffset(String arrayComponentKlass);

    /**
     * Returns the array index scale for an array of a given component type.
     * @param arrayComponentKlass component type
     * @return index scale
     */
    int arrayIndexScale(String arrayComponentKlass);

    /**
     * Returns the object alignment.
     * @return object alignment
     */
    int objectAlignment();

    /**
     * Returns the object header size.
     * @return header size
     */
    int objectHeaderSize();

    /**
     * Returns the array header size.
     * This includes the array length pseudofield.
     * @return array header size
     */
    int arrayHeaderSize();

    /**
     * Returns native address size.
     * @return address size in bytes
     */
    int addressSize();

    /**
     * Returns class pointer size.
     * @return class pointer size, in bytes
     */
    int classPointerSize();

    /**
     * Reads a boolean off the object at given offset.
     * @param obj instance
     * @param offset offset
     * @return the boolean
     */
    boolean getBoolean(Object obj, long offset);

    /**
     * Reads a byte off the object at given offset.
     * @param obj instance
     * @param offset offset
     * @return the byte
     */
    byte getByte(Object obj, long offset);

    /**
     * Reads a short off the object at given offset.
     * @param obj instance
     * @param offset offset
     * @return the short
     */
    short getShort(Object obj, long offset);

    /**
     * Reads a char off the object at given offset.
     * @param obj instance
     * @param offset offset
     * @return the char
     */
    char getChar(Object obj, long offset);

    /**
     * Reads an int off the object at given offset.
     * @param obj instance
     * @param offset offset
     * @return the int
     */
    int getInt(Object obj, long offset);

    /**
     * Reads a float off the object at given offset.
     * @param obj instance
     * @param offset offset
     * @return the float
     */
    float getFloat(Object obj, long offset);

    /**
     * Reads a long off the object at given offset.
     * @param obj instance
     * @param offset offset
     * @return the long
     */
    long getLong(Object obj, long offset);

    /**
     * Reads a double off the object at given offset.
     * @param obj instance
     * @param offset offset
     * @return the double
     */
    double getDouble(Object obj, long offset);

    /**
     * Reads an object off the object at given offset.
     * @param obj instance
     * @param offset offset
     * @return the Object
     */
    Object getObject(Object obj, long offset);

    /**
     * Returns the informational details about the current VM mode
     * @return String details
     */
    String details();
}
