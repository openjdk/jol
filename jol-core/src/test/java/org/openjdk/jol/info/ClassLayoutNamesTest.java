/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

package org.openjdk.jol.info;

import org.junit.Assert;
import org.junit.Test;

public class ClassLayoutNamesTest {

    static class A {
        int[] array = new int[10];
        B[] bs = new B[10];

        class B {

        }
    }

    @Test
    public void testPrintA() {
        String print = ClassLayout.parseInstance(new A()).toPrintable();
        Assert.assertTrue(print, print.contains("int[]"));
        Assert.assertTrue(print, print.contains(".A.B[]"));
        Assert.assertTrue(print, print.contains(".A.B"));
    }

    @Test
    public void testPrintB() {
        String print = ClassLayout.parseInstance(new A().new B()).toPrintable();
        Assert.assertTrue(print, print.contains(".A"));
    }

}
