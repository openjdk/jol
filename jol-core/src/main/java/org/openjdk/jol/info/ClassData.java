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

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openjdk.jol.util.ClassUtils;
import org.openjdk.jol.vm.ContendedSupport;

/**
 * Holds the class data, without the layout information.
 *
 * @author Aleksey Shipilev
 * @see org.openjdk.jol.layouters.Layouter
 */
public class ClassData {

    /**
     * Parse the existing instance.
     *
     * @param o object instance to parse
     * @return class data instance
     */
    public static ClassData parseInstance(Object o) {
        return parse(o, o.getClass());
    }

    /**
     * Parse the existing class.
     *
     * @param klass class to parse
     * @return class data instance
     */
    public static ClassData parseClass(Class klass) {
        return parse(null, klass);
    }

    private static int arrayLength(Object o) {
        if (o == null)
            return 0;
        Class<?> k = o.getClass();
        if (!k.isArray())
            throw new IllegalArgumentException(k.getName() + " is not an array class");
        if (k == byte[].class)
            return ((byte[]) o).length;
        if (k == boolean[].class)
            return ((boolean[]) o).length;
        if (k == short[].class)
            return ((short[]) o).length;
        if (k == char[].class)
            return ((char[]) o).length;
        if (k == int[].class)
            return ((int[]) o).length;
        if (k == float[].class)
            return ((float[]) o).length;
        if (k == double[].class)
            return ((double[]) o).length;
        if (k == long[].class)
            return ((long[]) o).length;
        return ((Object[])o).length;
    }

    private static ClassData parse(Object o, Class klass) {
        // If this is an array, do the array parsing, instead of ordinary class.
        if (klass.isArray()) {
            return new ClassData(o, klass.getName(), klass.getComponentType().getName(), arrayLength(o));
        }

        ClassData cd = new ClassData(o, klass.getName());
        Class superKlass = klass.getSuperclass();

        // TODO: Move to an appropriate constructor
        cd.isContended = ContendedSupport.isContended(klass);

        if (superKlass != null) {
            cd.addSuperClassData(klass.getSuperclass());
        }

        do {
            for (Field f : klass.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    cd.addField(FieldData.parse(f));
                }
            }
            cd.addSuperClass(ClassUtils.getSafeName(klass));
        } while ((klass = klass.getSuperclass()) != null);

        return cd;
    }

    private final WeakReference<Object> instance;
    private final String name;
    private final List<FieldData> fields;
    private final List<String> classNames;
    private final String arrayKlass;
    private final String arrayComponentKlass;
    private final long length;
    private final boolean isArray;
    private boolean isContended;
    private ClassData superClass;

    /**
     * Constructs the empty ClassData, suited for regular class.
     */
    public ClassData(String name) {
        this(null, name);
    }

    private ClassData(Object instance, String name) {
        this.instance = new WeakReference<Object>(instance);
        this.name = name;
        this.fields = new ArrayList<FieldData>();
        this.classNames = new ArrayList<String>();
        this.length = -1;
        this.arrayKlass = null;
        this.arrayComponentKlass = null;
        this.isArray = false;
        this.superClass = null;
        this.isContended = false;
    }

    /**
     * Constructs the empty ClassData, suited for arrays.
     *
     * @param arrayKlass      array class, e.g. "int[]"
     * @param componentKlass, e.g. "int"
     * @param length          array length
     */
    public ClassData(String arrayKlass, String componentKlass, int length) {
        this(null, arrayKlass, componentKlass, length);
    }

    private ClassData(Object instance, String arrayKlass, String componentKlass, int length) {
        this.instance = new WeakReference<Object>(instance);
        this.name = arrayKlass;
        this.arrayKlass = arrayKlass;
        this.arrayComponentKlass = componentKlass;
        this.fields = null;
        this.classNames = null;
        this.length = length;
        this.isArray = true;
        this.superClass = null;
        this.isContended = false;
    }

    /**
     * Add the super-class into the hierarchy.
     *
     * @param superClass super class name
     */
    public void addSuperClass(String superClass) {
        classNames.add(0, superClass);
    }

    /**
     * Add the super-class data of the class.
     *
     * @param superClass super class
     */
    public void addSuperClassData(Class superClass) {
        this.superClass = parseClass(superClass);
    }

    /**
     * Add the field data.
     *
     * @param fieldData the data to add
     */
    public void addField(FieldData fieldData) {
        fields.add(fieldData);
    }

    /**
     * Get the fields' data, including all the fields
     * in the hierarchy.
     *
     * @return field data
     */
    public Collection<FieldData> fields() {
        if (isArray) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(fields);
        }
    }

    /**
     * Get the fields' of the own fields.
     *
     * @return field data
     */
    public Collection<FieldData> ownFields() {
        return fieldsFor(classNames.get(classNames.size() - 1));
    }

    /**
     * Returns the count of the oops in th class
     *
     * @return oops count
     */
    public int oopsCount() {
        int count = 0;

        for (FieldData f : fields) {
            String simpleName = f.typeClass();

            if (
                !simpleName.equals("boolean") &&
                !simpleName.equals("byte") &&
                !simpleName.equals("short") &&
                !simpleName.equals("char") &&
                !simpleName.equals("int") &&
                !simpleName.equals("float") &&
                !simpleName.equals("long") &&
                !simpleName.equals("double")
            ) {
                count++;
            }
        }

        return count;
    }

    /**
     * Get the fields' data for the given class.
     *
     * @param klass class name
     * @return field data
     */
    public Collection<FieldData> fieldsFor(String klass) {
        List<FieldData> r = new ArrayList<FieldData>();
        for (FieldData f : fields) {
            if (f.hostClass().equals(klass)) {
                r.add(f);
            }
        }
        return r;
    }

    /**
     * Get the class names in the hierarchy,
     * starting from the superclasses down to subclasses
     *
     * @return list of class names
     */
    public List<String> classHierarchy() {
        if (isArray) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(classNames);
        }
    }

    /**
     * Answer class name
     *
     * @return string representation of class name
     */
    public String name() {
        return name;
    }

    /**
     * Is this class data for the array?
     *
     * @return true, if class data represents the array; false otherwise
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * Get ClassData of the super-class.
     *
     * @return ClassData
     */
    public ClassData superClass() {
        return superClass;
    }

    /**
     * Does the class have @Contended annotation?
     *
     * @return true, if class has @Contended annotation; false otherwise
     */
    public boolean isContended() {
        return isContended;
    }

    /**
     * Answer the array class for this class data.
     *
     * @return array class name, e.g. "int[]".
     */
    public String arrayClass() {
        if (!isArray) {
            throw new IllegalStateException("Asking array class for non-array ClassData");
        }
        return arrayKlass;
    }

    /**
     * Answer the array component class for this class data.
     *
     * @return array component class name, e.g. "int" for int[] array.
     */
    public String arrayComponentType() {
        if (!isArray) {
            throw new IllegalStateException("Asking array component type for non-array ClassData");
        }
        return arrayComponentKlass;
    }

    /**
     * Answer the array length for this class data.
     *
     * @return array length
     */
    public long arrayLength() {
        if (!isArray) {
            throw new IllegalStateException("Asking array length for non-array ClassData");
        }
        return length;
    }

    /**
     * Merge this class data with the super-class class data
     *
     * @param superClassData super class data
     */
    public void merge(ClassData superClassData) {
        fields.addAll(superClassData.fields);
        classNames.addAll(0, superClassData.classNames);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassData classData = (ClassData) o;

        if (isArray != classData.isArray) return false;
        if (length != classData.length) return false;
        if (arrayComponentKlass != null ? !arrayComponentKlass.equals(classData.arrayComponentKlass) : classData.arrayComponentKlass != null)
            return false;
        if (arrayKlass != null ? !arrayKlass.equals(classData.arrayKlass) : classData.arrayKlass != null) return false;
        if (classNames != null ? !classNames.equals(classData.classNames) : classData.classNames != null) return false;
        if (fields != null ? !fields.equals(classData.fields) : classData.fields != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fields != null ? fields.hashCode() : 0;
        result = 31 * result + (classNames != null ? classNames.hashCode() : 0);
        result = 31 * result + (arrayKlass != null ? arrayKlass.hashCode() : 0);
        result = 31 * result + (arrayComponentKlass != null ? arrayComponentKlass.hashCode() : 0);
        result = 31 * result + (int) (length ^ (length >>> 32));
        result = 31 * result + (isArray ? 1 : 0);
        return result;
    }

    /**
     * @return the recorded instance, if available
     */
    public Object instance() {
        return instance.get();
    }
}
