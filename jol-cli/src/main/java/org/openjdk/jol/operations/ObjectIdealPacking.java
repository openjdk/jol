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

import org.openjdk.jol.Operation;
import org.openjdk.jol.datamodel.CurrentDataModel;
import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.CurrentLayouter;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.RawLayouter;

import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

/**
 * @author Aleksey Shipilev
 */
public class ObjectIdealPacking implements Operation {

    @Override
    public String label() {
        return "idealpack";
    }

    @Override
    public String description() {
        return "Compute the object footprint under different field layout strategies.";
    }

    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.err.println("Expected one or more JAR file names.");
            System.exit(1);
        }

        DataModel model = new CurrentDataModel();

        for (String jarName : args) {
            try {
                System.err.println("Parsing " + jarName);

                URLClassLoader cl = URLClassLoader.newInstance(new URL[]{new URL("jar:file:" + jarName + "!/")});

                JarFile jarFile = new JarFile(jarName);
                Enumeration e = jarFile.entries();
                while (e.hasMoreElements()) {
                    JarEntry je = (JarEntry) e.nextElement();
                    String name = je.getName();
                    if (je.isDirectory()) continue;
                    if (!name.endsWith(".class")) continue;

                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    try {
                        Class klass = cl.loadClass(className);
                        ClassData cd = ClassData.parseClass(klass);
                        ClassLayout raw = new RawLayouter(model).layout(cd);
                        ClassLayout vm = new CurrentLayouter().layout(cd);

                        System.out.printf("%3d, %3d, %3d, ",
                                model.headerSize(),
                                raw.instanceSize(),
                                vm.instanceSize());

                        final boolean[] BOOLS = {false, true};
                        for (boolean hierarchyGaps : BOOLS) {
                            for (boolean superClassGaps : BOOLS) {
                                for (boolean autoAlign : BOOLS) {
                                    for (boolean compactFields : BOOLS) {
                                        for (int fieldAllocationStyle : new int[]{0, 1, 2}) {
                                            ClassLayout l = new HotSpotLayouter(model,
                                                    hierarchyGaps, superClassGaps, autoAlign,
                                                    compactFields, fieldAllocationStyle).layout(cd);
                                            System.out.printf("%3d, ", l.instanceSize());
                                        }
                                    }
                                }
                            }
                        }

                        System.out.printf("%s%n", klass.getName());
                    } catch (VerifyError t) {
                    } catch (IncompatibleClassChangeError t) {
                    } catch (SecurityException t) {
                    } catch (ClassFormatError t) {
                    } catch (ClassNotFoundException t) {
                    } catch (NoClassDefFoundError t) {
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                jarFile.close();
            } catch (ZipException t) {
            } catch (FileNotFoundException t) {
                // ignore
            }
        }
    }

}
