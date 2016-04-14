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
     *
     * This example generates PNG images in your current directory.
     */

    public static volatile Object sink;

    public static void main(String[] args) throws Exception {
        out.println(VM.current().details());

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

        Object obj = array;

        GraphLayout.parseInstance(obj).toImage("array-1-new.png");

        for (int c = 2; c <= 5; c++) {
            System.gc();
            GraphLayout.parseInstance(obj).toImage("array-" + c + "-before.png");
        }

        for (int c = 0; c < COUNT; c++) {
            if (Math.random() < 0.5) {
                array[c] = null;
            }
        }

        GraphLayout.parseInstance(obj).toImage("array-6-after.png");

        for (int c = 7; c <= 10; c++) {
            System.gc();
            GraphLayout.parseInstance(obj).toImage("array-" + c + "-after-gc.png");
        }
    }

}
