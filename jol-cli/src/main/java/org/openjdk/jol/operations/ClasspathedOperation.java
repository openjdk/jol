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
        Constructor<?> ctor = klass.getDeclaredConstructor();
        try {
            ctor.setAccessible(true);
        } catch (Exception e) {
            // Unable to make it accessible, try to invoke constructor anyway
        }
        return ctor.newInstance();
    }

    protected abstract void runWith(Class<?> klass) throws Exception;
}
