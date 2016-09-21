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
package org.openjdk.jol.operations;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jol.Operation;
import org.openjdk.jol.OptionFormatter;
import org.openjdk.jol.util.ClassUtils;
import org.openjdk.jol.vm.VM;

import java.lang.reflect.Constructor;
import java.util.List;

import static java.lang.System.getProperty;
import static java.lang.System.out;

public abstract class ClasspathedOperation implements Operation {

    public void run(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new OptionFormatter(label()));

        OptionSpec<String> optClassPath = parser.accepts("cp", "Additional classpath entries, where to look for the referenced classes.")
                .withRequiredArg().ofType(String.class).describedAs("classpath").withValuesSeparatedBy(getProperty("path.separator"));

        OptionSpec<String> optClasses = parser.nonOptions("Class names to work on.");

        List<String> classes;
        try {
            OptionSet set = parser.parse(args);
            classes = set.valuesOf(optClasses);
            if (classes.isEmpty()) {
                System.err.println("Need class name(s) as the arguments.");
                System.err.println();
                parser.printHelpOn(System.err);
                return;
            }

            if (set.has(optClassPath)) {
                ClassUtils.addClasspathEntries(optClassPath.values(set));
            }
        } catch (OptionException e) {
            parser.printHelpOn(System.err);
            return;
        }

        out.println(VM.current().details());

        for (String klassName : classes) {
            try {
                runWith(ClassUtils.loadClass(klassName));
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }

    protected Object tryInstantiate(Class<?> klass) throws Exception {
        // Try to invoke default constructor first.
        try {
            Constructor<?> ctor = klass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object o = ctor.newInstance();
            System.out.println("Instantiated the sample instance via default constructor.");
            System.out.println();
            return o;
        } catch (Exception e) {
            // Fall-through, let's try something else.
        }

        // Try to enumerate other constructors and push the default values to them
        Constructor<?>[] cnstrs = klass.getDeclaredConstructors();
        for (Constructor<?> ctor : cnstrs) {
            try {
                ctor.setAccessible(true);
                Class<?>[] types = ctor.getParameterTypes();

                Object[] args = new Object[types.length];
                for (int c = 0; c < types.length; c++) {
                    args[c] = makeDefaultValue(types[c]);
                }

                Object o = ctor.newInstance(args);
                System.out.println("Instantiated the sample instance via " + ctor);
                System.out.println();
                return o;
            } catch (Exception e) {
                // no dice, try the next constructor
            }
        }

        throw new InstantiationException("No matching (default) constructor, and no other constructor work.");
    }

    private static Object makeDefaultValue(Class<?> type) {
        if (type == boolean.class || type == Boolean.class)   return Boolean.FALSE;
        if (type == byte.class    || type == Byte.class)      return Byte.valueOf((byte) 0);
        if (type == short.class   || type == Short.class)     return Short.valueOf((short)0);
        if (type == char.class    || type == Character.class) return Character.valueOf((char)0);
        if (type == int.class     || type == Integer.class)   return Integer.valueOf(0);
        if (type == float.class   || type == Float.class)     return Float.valueOf(0f);
        if (type == long.class    || type == Long.class)      return Long.valueOf(0);
        if (type == double.class  || type == Double.class)    return Double.valueOf(0d);
        return null;
    }

    protected abstract void runWith(Class<?> klass) throws Exception;
}
