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

import java.io.PrintWriter;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_19_Promotion {

    /*
     * The example of object promotion.
     *
     * Once the object survives the garbage collections, it is getting
     * promoted to another generation. In this example, we can track
     * the addresses of the objects, as it changes over time.
     *
     * VM also needs to record the "age" (that is, the number of GC
     * cycles the object had survived) of the object somewhere, and
     * it is stored in mark word as well. See how particular mark word
     * bits change with each promotion.
     */

    static volatile Object sink;

    public static void main(String[] args) throws Exception {
        out.println(VMSupport.vmDetails());

        PrintWriter pw = new PrintWriter(System.out, true);

        Object o = new Object();

        ClassLayout layout = ClassLayout.parseClass(Object.class);

        long lastAddr = VMSupport.addressOf(o);
        pw.printf("Fresh object is at %x%n", lastAddr);

        int moves = 0;
        for (int i = 0; i < 100000; i++) {
            long cur = VMSupport.addressOf(o);
            if (cur != lastAddr) {
                moves++;
                pw.printf("*** Move %2d, object is at %x%n", moves, cur);
                out.println(layout.toPrintable(o));
                lastAddr = cur;
            }

            // make garbage
            for (int c = 0; c < 10000; c++) {
                sink = new Object();
            }
        }

        pw.close();
    }

}
