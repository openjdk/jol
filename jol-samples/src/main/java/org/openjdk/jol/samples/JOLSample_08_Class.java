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
public class JOLSample_08_Class {

    /*
     * Another example of special treatment for some fields.
     *
     * If you run this example, you can see the large gap in instance field block.
     * There are no Java fields that could claim that block, hence there are no
     * "hidden" fields, like in the example before. This time, VM "injects" some
     * of the fields into the Class, to store some of the meta-information there.
     *
     * See:
     *  http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/tip/src/share/vm/classfile/javaClasses.hpp
     *  http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/tip/src/share/vm/classfile/javaClasses.cpp
     */

    public static void main(String[] args) throws Exception {
        out.println(VMSupport.vmDetails());
        out.println(ClassLayout.parseClass(Class.class).toPrintable());
    }

}
