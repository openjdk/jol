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

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_05_InheritanceBarrier {

    /*
     * This example shows the HotSpot field layout quirk.
     * (Works best with 64-bit VMs)
     *
     * Even though we have the alignment gap before A.a field, HotSpot
     * does not claim it, because it does not track the gaps in the
     * already laid out superclasses. This yields the virtual
     * "inheritance barrier" between super- and sub-class fields blocks.
     *
     * See also:
     *    https://bugs.openjdk.java.net/browse/JDK-8024913
     */

    public static void main(String[] args) throws Exception {
        out.println(VM.current().details());
        out.println(ClassLayout.parseClass(C.class).toPrintable());
    }

    public static class A {
        long a;
    }

    public static class B extends A {
        long b;
    }

    public static class C extends B {
        long c;
        int d;
    }

}
