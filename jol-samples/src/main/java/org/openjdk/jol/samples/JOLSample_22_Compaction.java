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

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_22_Compaction {

    /*
     * This is the example how VM compacts the objects.
     *
     * This example generates PNG images. You can see the freshly
     * allocated and populated list has quite the sparse layout.
     * It happens because many temporary objects are allocated
     * while populating the list. The subsequent GCs compact the
     * list into the one or few dense blocks.
     */

    public static volatile Object sink;

    public static void main(String[] args) throws Exception {
        out.println(VMSupport.vmDetails());

        // allocate some objects to beef up generations
        for (int c = 0; c < 1000000; c++) {
            sink = new Object();
        }
        System.gc();

        List<String> list = new ArrayList<String>();
        for (int c = 0; c < 1000; c++) {
            list.add("Key" + c);
        }

        for (int c = 1; c <= 10; c++) {
            GraphLayout.parseInstance(list).toImage("list-" + c + ".png");
            System.gc();
        }
    }


}
