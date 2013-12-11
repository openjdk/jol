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

import java.io.PrintWriter;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_20_Roots {

    /*
     * The example how VM traverses the root sets.
     *
     * During the GC, object reachability graph should be traversed
     * starting from somewhere. The root set is the set of intrinsically
     * reachable objects. Static fields are the part of root set, local
     * variables are the part of root set as well.
     *
     * In this example, we build the "ring" of objects, and reference
     * only the single link from that ring from the local variable.
     * This will have the effect of having the different parts of ring
     * in the root set, which will, in the end, change the ring layout
     * in memory.
     */

    static volatile Object sink;

    public interface L {
        L link();
        void bind(L l);
    }

    public static abstract class AL implements L {
        L l;
        public L link() { return l; }
        public void bind(L l) { this.l = l; }
    }

    public static class L1 extends AL {}
    public static class L2 extends AL {}
    public static class L3 extends AL {}
    public static class L4 extends AL {}
    public static class L5 extends AL {}
    public static class L6 extends AL {}

    public static void main(String[] args) throws Exception {
        out.println(VMSupport.vmDetails());

        PrintWriter pw = new PrintWriter(System.out, true);

        // create links
        L l1 = new L1();
        L l2 = new L2();
        L l3 = new L3();
        L l4 = new L4();
        L l5 = new L5();
        L l6 = new L6();

        // bind the ring
        l1.bind(l2);
        l2.bind(l3);
        l3.bind(l4);
        l4.bind(l5);
        l5.bind(l6);
        l6.bind(l1);

        // current root
        L r = l1;

        // break all other roots
        l1 = l2 = l3 = l4 = l5 = l6 = null;

        long lastAddr = VMSupport.addressOf(r);
        pw.printf("Fresh object is at %x%n", lastAddr);

        int moves = 0;
        for (int i = 0; i < 100000; i++) {

            // scan for L1 and determine it's address
            L s = r;
            while (!((s = s.link()) instanceof L1)) ;

            long cur = VMSupport.addressOf(s);
            s = null;

            // if L1 had moved, then probably the entire ring had also moved
            if (cur != lastAddr) {
                moves++;
                pw.printf("*** Move %2d, L1 is at %x%n", moves, cur);
                pw.println("*** Root is " + r.getClass());

                pw.println(GraphLayout.parseInstance(r).toPrintable());

                // select another link
                for (int c = 0; c < ThreadLocalRandom.current().nextInt(100); c++) {
                    r = r.link();
                }

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
