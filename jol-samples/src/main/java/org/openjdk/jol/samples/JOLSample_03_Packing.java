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
package org.openjdk.jol.samples;

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.util.VMSupport;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_03_Packing {

    /*
     * This is the example how VM packs the fields.
     *
     * JVMs pack the fields to minimize the memory footprint. Run
     * this example and see the fields are densely packed, and gaps
     * are minimal. It is achieved by aligning fields in 8->4->2->1
     * order, because it can not break the initial alignment, once we
     * align the 8-byte field. The gap resulted in the initial 8-byte
     * align can be taken by one or few smaller-sized fields.
     *
     * Note that the actual field order is very different from the
     * declared order. Nothing in the JVM spec requires otherwise.
     */

    public static void main(String[] args) throws Exception {
        out.println(VMSupport.vmDetails());
        out.println(ClassLayout.parseClass(A.class).toPrintable());
    }

    public static class A {
        boolean bo1, bo2;
        byte b1, b2;
        char c1, c2;
        double d1, d2;
        float f1, f2;
        int i1, i2;
        long l1, l2;
        short s1, s2;
    }

}
