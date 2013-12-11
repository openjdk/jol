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

import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_13_BiasedLocking {

    /*
     * This is the example of biased locking.
     *
     * In order to demonstrate this, we first need to sleep for >5 seconds
     * to pass the grace period of biased locking. Then, we do the same
     * trick as the example before. You may notice that the mark word
     * had not changed after the lock was released. That is because
     * the mark word now contains the reference to the thread this object
     * was biased to.
     */

    public static void main(String[] args) throws Exception {
        out.println(VMSupport.vmDetails());

        TimeUnit.SECONDS.sleep(6);

        final A a = new A();

        ClassLayout layout = ClassLayout.parseClass(A.class);

        out.println("**** Fresh object");
        out.println(layout.toPrintable(a));

        synchronized (a) {
            out.println("**** With the lock");
            out.println(layout.toPrintable(a));
        }

        out.println("**** After the lock");
        out.println(layout.toPrintable(a));
    }

    public static class A {
        // no fields
    }

}
