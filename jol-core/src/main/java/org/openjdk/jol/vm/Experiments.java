/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jol.vm;

class Experiments {

    public static class CompressedOopsClass {
        public Object obj1;
        public Object obj2;
    }

    public static class HeaderClass {
        public boolean b1;
    }

    public static class MyObject0 {

    }

    public static class MyObject1 {

    }

    public static class MyObject2 {
        private boolean b;
    }

    public static class MyObject3 {
        private int i;
    }

    public static class MyObject4 {
        private long l;
    }

    public static class MyObject5 {
        private Object o;
    }

    public static class MyBooleans4 {
        private boolean f1, f2, f3, f4;
    }

    public static class MyBytes4 {
        private byte f1, f2, f3, f4;
    }

    public static class MyShorts4 {
        private short f1, f2, f3, f4;
    }

    public static class MyChars4 {
        private char f1, f2, f3, f4;
    }

    public static class MyInts4 {
        private int f1, f2, f3, f4;
    }

    public static class MyFloats4 {
        private float f1, f2, f3, f4;
    }

    public static class MyLongs4 {
        private long f1, f2, f3, f4;
    }

    public static class MyDoubles4 {
        private double f1, f2, f3, f4;
    }
}