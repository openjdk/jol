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
package org.openjdk.jol.datamodel;

/**
 * x86 data model, 64 bits.
 *
 * @author Aleksey Shipilev
 */
public class X86_64_DataModel implements DataModel {

    private final int align;

    public X86_64_DataModel() {
        this(8);
    }

    public X86_64_DataModel(int align) {
        this.align = align;
    }

    @Override
    public int headerSize() {
        // 8 byte mark + 8 byte class
        return 16;
    }

    @Override
    public int arrayHeaderSize() {
        return headerSize() + 4;
    }

    @Override
    public int sizeOf(String klass) {
        if (klass.equals("byte"))    return 1;
        if (klass.equals("boolean")) return 1;
        if (klass.equals("short"))   return 2;
        if (klass.equals("char"))    return 2;
        if (klass.equals("int"))     return 4;
        if (klass.equals("float"))   return 4;
        if (klass.equals("long"))    return 8;
        if (klass.equals("double"))  return 8;
        return 8;
    }

    @Override
    public int objectAlignment() {
        return align;
    }

    @Override
    public String toString() {
        return "X64 model, " + align + "-byte aligned";
    }

}
