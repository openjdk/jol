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

package org.openjdk.jol.util;

import org.junit.Assert;
import org.junit.Test;

public class ClassUtilsTest {

    @Test
    public void testSingle() {
        Assert.assertEquals("<null>",        ClassUtils.binaryToHuman(null));

        Assert.assertEquals("<error>",       ClassUtils.binaryToHuman(""));
        Assert.assertEquals("<error>",       ClassUtils.binaryToHuman("["));
        Assert.assertEquals("<error>",       ClassUtils.binaryToHuman("[["));
        Assert.assertEquals("<error>",       ClassUtils.binaryToHuman("[[["));

        Assert.assertEquals("boolean",       ClassUtils.binaryToHuman("Z"));
        Assert.assertEquals("boolean[]",     ClassUtils.binaryToHuman("[Z"));
        Assert.assertEquals("boolean[][]",   ClassUtils.binaryToHuman("[[Z"));
        Assert.assertEquals("boolean[][][]", ClassUtils.binaryToHuman("[[[Z"));

        Assert.assertEquals("byte",          ClassUtils.binaryToHuman("B"));
        Assert.assertEquals("byte[]",        ClassUtils.binaryToHuman("[B"));
        Assert.assertEquals("byte[][]",      ClassUtils.binaryToHuman("[[B"));
        Assert.assertEquals("byte[][][]",    ClassUtils.binaryToHuman("[[[B"));

        Assert.assertEquals("char",          ClassUtils.binaryToHuman("C"));
        Assert.assertEquals("char[]",        ClassUtils.binaryToHuman("[C"));
        Assert.assertEquals("char[][]",      ClassUtils.binaryToHuman("[[C"));
        Assert.assertEquals("char[][][]",    ClassUtils.binaryToHuman("[[[C"));

        Assert.assertEquals("short",         ClassUtils.binaryToHuman("S"));
        Assert.assertEquals("short[]",       ClassUtils.binaryToHuman("[S"));
        Assert.assertEquals("short[][]",     ClassUtils.binaryToHuman("[[S"));
        Assert.assertEquals("short[][][]",   ClassUtils.binaryToHuman("[[[S"));

        Assert.assertEquals("int",           ClassUtils.binaryToHuman("I"));
        Assert.assertEquals("int[]",         ClassUtils.binaryToHuman("[I"));
        Assert.assertEquals("int[][]",       ClassUtils.binaryToHuman("[[I"));
        Assert.assertEquals("int[][][]",     ClassUtils.binaryToHuman("[[[I"));

        Assert.assertEquals("float",         ClassUtils.binaryToHuman("F"));
        Assert.assertEquals("float[]",       ClassUtils.binaryToHuman("[F"));
        Assert.assertEquals("float[][]",     ClassUtils.binaryToHuman("[[F"));
        Assert.assertEquals("float[][][]",   ClassUtils.binaryToHuman("[[[F"));

        Assert.assertEquals("long",          ClassUtils.binaryToHuman("J"));
        Assert.assertEquals("long[]",        ClassUtils.binaryToHuman("[J"));
        Assert.assertEquals("long[][]",      ClassUtils.binaryToHuman("[[J"));
        Assert.assertEquals("long[][][]",    ClassUtils.binaryToHuman("[[[J"));

        Assert.assertEquals("double",        ClassUtils.binaryToHuman("D"));
        Assert.assertEquals("double[]",      ClassUtils.binaryToHuman("[D"));
        Assert.assertEquals("double[][]",    ClassUtils.binaryToHuman("[[D"));
        Assert.assertEquals("double[][][]",  ClassUtils.binaryToHuman("[[[D"));

        Assert.assertEquals("java.lang.Object",       ClassUtils.binaryToHuman("java/lang/Object"));
        Assert.assertEquals("java.lang.Object[]",     ClassUtils.binaryToHuman("[Ljava/lang/Object;"));
        Assert.assertEquals("java.lang.Object[][]",   ClassUtils.binaryToHuman("[[Ljava/lang/Object;"));
        Assert.assertEquals("java.lang.Object[][][]", ClassUtils.binaryToHuman("[[[Ljava/lang/Object;"));

        Assert.assertEquals("java.util.HashMap$Entry",       ClassUtils.binaryToHuman("java/util/HashMap$Entry"));
        Assert.assertEquals("java.util.HashMap$Entry[]",     ClassUtils.binaryToHuman("[Ljava/util/HashMap$Entry;"));
        Assert.assertEquals("java.util.HashMap$Entry[][]",   ClassUtils.binaryToHuman("[[Ljava/util/HashMap$Entry;"));
        Assert.assertEquals("java.util.HashMap$Entry[][][]", ClassUtils.binaryToHuman("[[[Ljava/util/HashMap$Entry;"));
    }

}
