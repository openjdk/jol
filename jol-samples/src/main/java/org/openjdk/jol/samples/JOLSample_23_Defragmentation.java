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

import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.util.VMSupport;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_23_Defragmentation {

    /*
     * This is the example how VM defragments the heap.
     *
     * In this example, we have the array of objects, which
     * is densely allocated, and survives multiple GCs as
     * the dense structure. Then, we randomly purge half of
     * the elements. Now the memory layout is sparse. Subsequent
     * GCs take care of that.
     */

    public static volatile Object sink;

    public static void main(String[] args) throws Exception {
        out.println(VMSupport.vmDetails());

        // allocate some objects to beef up generations
        for (int c = 0; c < 1000000; c++) {
            sink = new Object();
        }
        System.gc();

        final int COUNT = 10000;

        Object[] array = new Object[COUNT];
        for (int c = 0; c < COUNT; c++) {
            array[c] = new Object();
        }

        GraphLayout.parseInstance(array).toImage("array-1-new.png");

        for (int c = 2; c <= 5; c++) {
            System.gc();
            GraphLayout.parseInstance(array).toImage("array-" + c + "-before.png");
        }

        for (int c = 0; c < COUNT; c++) {
            if (Math.random() < 0.5) {
                array[c] = null;
            }
        }

        GraphLayout.parseInstance(array).toImage("array-6-after.png");

        for (int c = 7; c <= 10; c++) {
            System.gc();
            GraphLayout.parseInstance(array).toImage("array-" + c + "-after-gc.png");
        }
    }

}
