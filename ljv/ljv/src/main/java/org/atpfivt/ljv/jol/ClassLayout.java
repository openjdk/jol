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
package org.atpfivt.ljv.jol;

import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.info.ClassData;

import java.util.Objects;
import java.util.SortedSet;

/**
 * Handles the class data *with* the layout information.
 */
public class ClassLayout {

    /**
     * Produce the class layout for the given class.
     *
     * This is a shortcut for {@link #parseClass(Class, Layouter)},
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
     * Note: You can use this method to cache the introspection results for a constant-sized
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


    private final ClassData classData;
    private final SortedSet<FieldLayout> fields;
    private final DataModel model;
    private final long size;

    private ClassLayout(ClassData classData, SortedSet<FieldLayout> fields, DataModel model, long instanceSize, int lossesInternal, int lossesExternal, int lossesTotal) {
        this.classData = classData;
        this.fields = fields;
        this.model = model;
        this.size = instanceSize;
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
        long next = model.headerSize();
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (FieldLayout f : fields()) {
            sb.append(f).append("\n");
        }
        sb.append("size = ").append(size).append("\n");
        return sb.toString();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassLayout that = (ClassLayout) o;
        return fields.equals(that.fields) &&
                model.equals(that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, model);
    }
}
