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
import org.openjdk.jol.util.VMSupport;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.getProperty;
import static java.lang.System.out;

public abstract class ClasspathedOPeration implements Operation {

    public void run(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        parser.formatHelpWith(new OptionFormatter(label()));

        OptionSpec<String> optClassPath = parser.accepts("cp", "Additional classpath entries, where to look for the referenced classes.")
                .withRequiredArg().ofType(String.class).describedAs("classpath").withValuesSeparatedBy(getProperty("path.separator"));

        OptionSpec<String> optClasses = parser.nonOptions("Class names to work on.");

        List<String> classes;
        URL[] classPath;
        try {
            OptionSet set = parser.parse(args);
            classes = set.valuesOf(optClasses);

            List<URL> cp = new ArrayList<URL>();
            for (String cpEntry : set.valuesOf(optClassPath)) {
                cp.add(new File(cpEntry).toURI().toURL());
            }
            classPath = cp.toArray(new URL[0]);
        } catch (OptionException e) {
            parser.printHelpOn(System.err);
            return;
        }

        out.println(VMSupport.vmDetails());

        URLClassLoader cl = new URLClassLoader(classPath, ClassLoader.getSystemClassLoader());

        for (String klassName : classes) {
            try {
                runWith(Class.forName(klassName, true, cl));
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }

    protected abstract void runWith(Class<?> klass) throws Exception;
}
