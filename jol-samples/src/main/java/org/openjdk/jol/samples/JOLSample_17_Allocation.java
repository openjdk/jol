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



import org.openjdk.jol.vm.VM;

import java.io.PrintWriter;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_17_Allocation {

    /*
     * The example of allocation addresses.
     *
     * This example shows the addresses of newly allocated objects
     * grow linearly in HotSpot. This is because the allocation in
     * parallel collectors is linear. We can also see it rewinds back
     * to the same offsets -- that's the start of some GC generation.
     * The address of the generation is changing, while GC adjusts
     * for the allocation rate.
     */

    public static void main(String[] args) throws Exception {
        out.println(VM.current().details());

        PrintWriter pw = new PrintWriter(out, true);

        long last = VM.current().addressOf(new Object());
        for (int l = 0; l < 1000 * 1000 * 1000; l++) {
            long current = VM.current().addressOf(new Object());

            long distance = Math.abs(current - last);
            if (distance > 16 * 1024) {
                pw.printf("Jumping from %x to %x (distance = %d bytes, %dK, %dM)%n",
                        last,
                        current,
                        distance,
                        distance / 1024,
                        distance / 1024 / 1024);
            }

            last = current;
        }

        pw.close();
    }

}
