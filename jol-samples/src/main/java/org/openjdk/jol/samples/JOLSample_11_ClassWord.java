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

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_11_ClassWord {

    /*
     * This is the example to have insight into object headers.
     *
     * In HotSpot, object header consists of two parts: mark word,
     * and class word. We can clearly see the class word by analysing
     * two syntactically equivalent instances of two distinct classes.
     * See the difference in headers, that difference is the reference
     * to class.
     */

    public static void main(String[] args) throws Exception {
        out.println(VMSupport.vmDetails());
        out.println(ClassLayout.parseClass(A.class).toPrintable(new A()));
        out.println(ClassLayout.parseClass(B.class).toPrintable(new B()));
    }

    public static class A {
        // no fields
    }

    public static class B {
        // no fields
    }

}
