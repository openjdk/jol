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

/**
 * Utility class for normalizing ZGC addresses by removing color bits
 *
 * @see <a href="https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/z/zAddress.hpp">Layout description</a>
 */
class ZGCAddress {
    private static final long REMAPPED_BITS_MASK = 0b1111L << 12;
    private static final long CLEAR_UNUSED_BITS_MASK = (1L << 46) - 1;
    private static final long COLOR_BITS_COUNT = 16;
    private static final boolean IS_ARM64 = "aarch64".equals(System.getProperty("os.arch"));

    static long uncolorize(long address) {
        return IS_ARM64 ? uncolorizeAarch(address) : uncolorizeNonAarch(address);
    }

    private static long uncolorizeNonAarch(long address) {
        int shift = Long.numberOfTrailingZeros(address & REMAPPED_BITS_MASK) + 1;
        return (address >> shift) & CLEAR_UNUSED_BITS_MASK;
    }

    private static long uncolorizeAarch(long address) {
        return (address >> COLOR_BITS_COUNT) & CLEAR_UNUSED_BITS_MASK;
    }

    private ZGCAddress() {}
}
