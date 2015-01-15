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

import java.lang.reflect.Constructor;

import org.openjdk.jol.Operation;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.util.VMSupport;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class ObjectInternals implements Operation {

    @Override
    public String label() {
        return "internals";
    }

    @Override
    public String description() {
        return "Show the object internals: field layout and default contents, object header";
    }

    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Expected one or more class names.");
            return;
        }
        out.println(VMSupport.vmDetails());

        for (String klassName : args) {
            try {
                Class<?> klass = Class.forName(klassName);
                try {
                    Constructor<?> ctor = klass.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    Object o = klass.newInstance();
                    out.println(ClassLayout.parseClass(klass).toPrintable(o));
                } catch (NoSuchMethodException e) {
                    out.println("VM fails to invoke the default constructor, falling back to class-only introspection.");
                    out.println();
                    out.println(ClassLayout.parseClass(klass).toPrintable());
                } catch (IllegalAccessException e) {
                    out.println("VM fails to invoke the default constructor, falling back to class-only introspection.");
                    out.println();
                    out.println(ClassLayout.parseClass(klass).toPrintable());
                } catch (InstantiationException e) {
                    out.println("VM fails to invoke the default constructor, falling back to class-only introspection.");
                    out.println();
                    out.println(ClassLayout.parseClass(klass).toPrintable());
                }
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }

}
