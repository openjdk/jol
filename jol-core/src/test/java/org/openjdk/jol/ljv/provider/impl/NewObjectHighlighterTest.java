/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jol.ljv.provider.impl;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.ljv.VersionGuardedTest;

import static org.junit.Assume.assumeTrue;
import static org.openjdk.jol.ljv.provider.impl.NewObjectHighlighter.HIGHLIGHT;

public class NewObjectHighlighterTest extends VersionGuardedTest {
    @Test
    public void newObjectsAreHighlighted() {
        assumeTrue(is11());
        Object o1 = new Object();
        Object o2 = new Object();
        NewObjectHighlighter highlighter = new NewObjectHighlighter();
        Assert.assertEquals(HIGHLIGHT, highlighter.getAttribute(o1));
        Assert.assertEquals(HIGHLIGHT, highlighter.getAttribute(o2));
        Assert.assertEquals("", highlighter.getAttribute(o1));
    }
}
