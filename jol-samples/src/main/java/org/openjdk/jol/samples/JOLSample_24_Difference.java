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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_24_Difference {

    /*
     * This is the example how would one use the GraphLayout differences to
     * figure out the object graph changes.
     *
     * Here, we have the ConcurrentHashMap, and three measurements:
     *   1) The initial CHM that has no backing storage;
     *   2) After adding the first KV pair, when both KV pair is allocated,
     *      and the backing storage is allocated;
     *   3) After adding the second KV pair.
     *
     * An API for subtracting the GraphLayouts helps to show the difference
     * between the snapshots. Note that differences are based on object
     * addresses, so if GC moves under our feet, the difference is unreliable.
     * It is a good idea to keep the allocations at minimum between the snapshots.
     */

    public static void main(String[] args) throws Exception {
        out.println(VM.current().details());

        Map<String, String> chm = new ConcurrentHashMap<String, String>();

        GraphLayout gl1 = GraphLayout.parseInstance(chm);

        chm.put("Foo", "Bar");
        GraphLayout gl2 = GraphLayout.parseInstance(chm);

        chm.put("Foo2", "Bar2");
        GraphLayout gl3 = GraphLayout.parseInstance(chm);

        System.out.println(gl2.subtract(gl1).toPrintable());
        System.out.println(gl3.subtract(gl2).toPrintable());
    }

}
