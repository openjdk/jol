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
package org.openjdk.jol.util;

public class MathUtil {

    public static int log2p(int x) {
        int r = 0;
        while ((x >>= 1) != 0) {
            r++;
        }
        return r;
    }

    public static int minDiff(int... offs) {
        int min = Integer.MAX_VALUE;
        for (int o1 : offs) {
            for (int o2 : offs) {
                if (o1 != o2) {
                    min = Math.min(min, Math.abs(o1 - o2));
                }
            }
        }
        return min;
    }

    public static long gcd(long a, long b) {
        while (b > 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static int pow2(int power) {
        int p = 1;
        for (int i = 0; i < power; i++) {
            p *= 2;
        }
        return p;
    }

    /**
     * Aligns the argument to the given alignment.
     * Alignment should be a power of two.
     *
     * @param v value to align
     * @param a alignment, should be power of two
     * @return aligned value
     */
    public static int align(int v, int a) {
        return (v + a - 1) & -a;
    }

    /**
     * Aligns the argument to the given alignment.
     * Alignment should be a power of two.
     *
     * @param v value to align
     * @param a alignment, should be power of two
     * @return aligned value
     */
    public static long align(long v, int a) {
        return (v + a - 1) & -a;
    }

    /**
     * Computes the human-readable string representation of percent ratio of value/base.
     *
     * @param value value to compute from
     * @param base base to compute from
     * @return human-readable representation
     */
    public static String diffPercent(long value, long base) {
        if (value == base) {
            return "0%";
        } else {
            double v = 100D * value / base - 100D;
            if (Math.abs(v) < 0.1) {
                return "~0%";
            } else {
                return String.format("%+.1f%%", v);
            }
        }
    }

    /**
     * Returns the value formatted in proper units. Assumes power-of-ten suffixes.
     *
     * @param value value to compute from
     * @return string representation of the value
     */
    public static String inProperUnits(long value) {
        final long K = 1000L;
        final long M = K * K;
        final long G = K * K * K;
        if (value > 100 * G) {
            return (value / G) + "G";
        } else if (value > 100 * M) {
            return (value / M) + "M";
        } else if (value > 100 * K) {
            return (value / K) + "K";
        } else {
            return value + "";
        }
    }

}
