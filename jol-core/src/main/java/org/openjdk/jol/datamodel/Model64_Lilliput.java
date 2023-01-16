/*
 * Copyright (c) 2023, Red Hat, Inc. All rights reserved.
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
package org.openjdk.jol.datamodel;

/**
 * 64 bits, Lilliput (Experimental)
 *
 * @author Aleksey Shipilev
 */
public class Model64_Lilliput implements DataModel {

    private final int align;
    private final boolean compRefs;
    private final boolean target;

    public Model64_Lilliput() {
        this(false, 8, false);
    }

    public Model64_Lilliput(boolean compRefs, int align, boolean target) {
        this.compRefs = compRefs;
        this.align = align;
        this.target = target;
    }

    @Override
    public int markHeaderSize() {
        return target ? 1 : 8;
    }

    @Override
    public int classHeaderSize() {
        return target ? 3 : 0;
    }

    @Override
    public int arrayLengthHeaderSize() {
        return 4;
    }

    @Override
    public int headerSize() {
        return markHeaderSize() + classHeaderSize();
    }

    @Override
    public int arrayHeaderSize() {
        return headerSize() + arrayLengthHeaderSize();
    }

    @Override
    public int sizeOf(String klass) {
        switch (klass) {
            case "byte":
            case "boolean":
                return 1;
            case "short":
            case "char":
                return 2;
            case "int":
            case "float":
                return 4;
            case "long":
            case "double":
                return 8;
            default:
                return (compRefs ? 4 : 8);
        }
    }

    @Override
    public int objectAlignment() {
        return align;
    }

    @Override
    public String toString() {
        return "64-bit model" +
                ", Lilliput (" + (target ? "ultimate target" : "current experiment") + ")" +
                ", " + (compRefs ? "" : "NO ") + "compressed references" +
                ", compressed classes" +
                ", " + align + "-byte aligned";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Model64_Lilliput that = (Model64_Lilliput) o;
        return align == that.align;
    }

    @Override
    public int hashCode() {
        return align;
    }
}
