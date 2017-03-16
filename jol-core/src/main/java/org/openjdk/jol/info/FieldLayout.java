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

/**
 * Holds the field info with the layout.
 *
 * @author Aleksey Shipilev
 */
public class FieldLayout implements Comparable<FieldLayout> {
    private final FieldData f;
    private final long size;
    private final long offset;

    /**
     * Construct field layout with discovered offset and field size.
     *
     * @param fieldData field data for the field
     * @param offset    discovered offset
     * @param size      discovered field size
     */
    public FieldLayout(FieldData fieldData, long offset, long size) {
        this.f = fieldData;
        this.size = size;
        this.offset = offset;
    }

    /**
     * Answers field offset
     *
     * @return field offset
     */
    public long offset() {
        return offset;
    }

    /**
     * Answers field size
     *
     * @return field size
     */
    public long size() {
        return size;
    }

    /**
     * Answers the class for the field type
     *
     * @return string representation of field type
     */
    public String typeClass() {
        return f.typeClass();
    }

    /**
     * Answers the class for the field holder.
     *
     * @return string representation of holder class
     */
    public String hostClass() {
        return f.hostClass();
    }

    /**
     * Answers the field name.
     *
     * @return field name
     */
    public String name() {
        return f.name();
    }

    public String shortFieldName() {
        String cl = hostClass();
        int idx = cl.lastIndexOf(".");
        if (idx != -1 && idx < cl.length()) {
            return cl.substring(idx + 1) + "." + f.name();
        } else {
            return cl + "." + name();
        }
    }

    FieldData data() {
        return f;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldLayout that = (FieldLayout) o;

        if (offset != that.offset) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int)(offset ^ (offset >>> 32));
    }

    @Override
    public int compareTo(FieldLayout o) {
        return Long.valueOf(offset).compareTo(o.offset);
    }

    @Override
    public String toString() {
        return f.hostClass() + "." + f.name() + " @" + offset + " (" + typeClass() + ", " + size + "b)";
    }

}
