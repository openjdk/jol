/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jol.samples;

import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;

import java.io.PrintWriter;
import java.util.Random;

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
        out.println(VM.current().details());

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

        long lastAddr = VM.current().addressOf(r);
        pw.printf("Fresh object is at %x%n", lastAddr);

        int moves = 0;
        for (int i = 0; i < 100000; i++) {

            // scan for L1 and determine it's address
            L s = r;
            while (!((s = s.link()) instanceof L1)) ;

            long cur = VM.current().addressOf(s);
            s = null;

            // if L1 had moved, then probably the entire ring had also moved
            if (cur != lastAddr) {
                moves++;
                pw.printf("*** Move %2d, L1 is at %x%n", moves, cur);
                pw.println("*** Root is " + r.getClass());

                pw.println(GraphLayout.parseInstance(r).toPrintable());

                // select another link
                Random random = new Random();
                for (int c = 0; c < random.nextInt(100); c++) {
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
