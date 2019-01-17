/*
 * Copyright (c) 2016, Red Hat Inc. All rights reserved.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClassUtils {

    private static volatile ClassLoader CL;

    static {
        // default class loader is our own ClassLoader
        CL = ClassUtils.class.getClassLoader();
    }

    /**
     * Add these new classpath entries to resolve against.
     * @param cpEntries classpath entries.
     */
    public static void addClasspathEntries(Collection<String> cpEntries) {
        List<URL> cp = new ArrayList<>();
        for (String cpEntry : cpEntries) {
            try {
                cp.add(new File(cpEntry).toURI().toURL());
            } catch (MalformedURLException e) {
                // don't care
            }
        }
        URL[] classPath = cp.toArray(new URL[0]);
        CL = new URLClassLoader(classPath, CL);
    }

    /**
     * Load class through our application classpath, plus optionally try to resolve
     * from the additional classpath.
     *
     * @param name class name
     * @return class
     * @throws ClassNotFoundException if class was not found
     */
    public static Class<?> loadClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, CL);
    }

    /**
     * Load class through the system classloader. This does not use additional classpath
     * for class resolution.
     *
     * @param name class name
     * @return class
     * @throws ClassNotFoundException if class was not found
     */
    public static Class<?> loadSystemClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, ClassLoader.getSystemClassLoader());
    }

    public static String getSafeName(Class klass) {
        // We want a human-readable class name. getName() returns JVM signature.
        // getCanonicalName() returns proper string, unless it is hits the bug.
        // If it fails, then we will fall back to getName()
        //   https://bugs.openjdk.java.net/browse/JDK-8057919
        try {
            String n = klass.getCanonicalName();
            if (n != null) {
                return n;
            }
        } catch (Throwable e) {
            // fall-through
        }
        return klass.getName();
    }
}
