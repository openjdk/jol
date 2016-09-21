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
package org.openjdk.jol.operations;

import org.openjdk.jol.info.ClassLayout;

import java.lang.reflect.Constructor;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class ObjectInternals extends ClasspathedOperation {

    @Override
    public String label() {
        return "internals";
    }

    @Override
    public String description() {
        return "Show the object internals: field layout and default contents, object header";
    }

    public void runWith(Class<?> klass) throws Exception {
        try {
            Object o = tryInstantiate(klass);
            out.println(ClassLayout.parseInstance(o).toPrintable());
        } catch (NoSuchMethodException e) {
            out.println("Failed to find matching constructor, falling back to class-only introspection.");
            out.println();
            out.println(ClassLayout.parseClass(klass).toPrintable());
        } catch (IllegalAccessException e) {
            out.println("Failed to find matching constructor, falling back to class-only introspection.");
            out.println();
            out.println(ClassLayout.parseClass(klass).toPrintable());
        } catch (InstantiationException e) {
            out.println("Failed to find matching constructor, falling back to class-only introspection.");
            out.println();
            out.println(ClassLayout.parseClass(klass).toPrintable());
        }
    }
}
