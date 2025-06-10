/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ZGCAddressTest {
    @Test
    public void testGenerationalZGCAddressUncolorize() {
        final long[] coloredAddresses;
        if ("aarch64".equals(System.getProperty("os.arch"))) {
            coloredAddresses = new long[] {
                // remapped bits inverted, no address and color overlap
                0b00100100_00000000_00000000_00111011_10100110_10111000_11100101_00010000L,
                0b00100100_00000000_00000000_00111011_10100110_10111000_11010101_00010000L,
                0b00100100_00000000_00000000_00111011_10100110_10111000_10110101_00010000L,
                0b00100100_00000000_00000000_00111011_10100110_10111000_01110101_00010000L
            };
        } else {
            coloredAddresses = new long[] {
                0b00100100_00000000_00000000_00111011_10100110_10111000_10000101_00010000L,
                0b00010010_00000000_00000000_00011101_11010011_01011100_01000101_00010000L,
                0b00001001_00000000_00000000_00001110_11101001_10101110_00100101_00010000L,
                0b00000100_10000000_00000000_00000111_01110100_11010111_00010101_00010000L,
            };
        }

        for (long address : coloredAddresses) {
            assertEquals(
                    0b100100_00000000_00000000_00111011_10100110_10111000L,
                    ZGCAddress.uncolorize(address)
            );
        }
    }
}
