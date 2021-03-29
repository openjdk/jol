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
package org.openjdk.jol.vm.sa;

/**
 * {@link Result} implementation for representing compressed reference informations as result.
 *
 * @author Serkan Ozal
 */
@SuppressWarnings("serial")
public class UniverseData implements Result {

    private final int addressSize;
    private final int objectAlignment;

    private final int oopSize;
    private final boolean compressedOopsEnabled;
    private final long narrowOopBase;
    private final int narrowOopShift;

    private final boolean compressedKlassPtrsEnabled;
    private final long narrowKlassBase;
    private final int narrowKlassShift;

    public UniverseData(int addressSize, int objectAlignment, int oopSize,
                        boolean compressedOopsEnabled, long narrowOopBase, int narrowOopShift,
                        boolean compressedKlassPtrsEnabled, long narrowKlassBase, int narrowKlassShift) {
        this.addressSize = addressSize;
        this.objectAlignment = objectAlignment;
        this.oopSize = oopSize;
        this.compressedOopsEnabled = compressedOopsEnabled;
        this.narrowOopBase = narrowOopBase;
        this.narrowOopShift = narrowOopShift;
        this.compressedKlassPtrsEnabled = compressedKlassPtrsEnabled;
        this.narrowKlassBase = narrowKlassBase;
        this.narrowKlassShift = narrowKlassShift;
    }

    public int getAddressSize() {
        return addressSize;
    }

    public int getObjectAlignment() {
        return objectAlignment;
    }

    public int getOopSize() {
        return oopSize;
    }

    public boolean isCompressedOopsEnabled() {
        return compressedOopsEnabled;
    }

    public long getNarrowOopBase() {
        return narrowOopBase;
    }

    public int getNarrowOopShift() {
        return narrowOopShift;
    }

    public boolean isCompressedKlassPtrsEnabled() {
        return compressedKlassPtrsEnabled;
    }

    public long getNarrowKlassBase() {
        return narrowKlassBase;
    }

    public int getNarrowKlassShift() {
        return narrowKlassShift;
    }

}
