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

                        ClassLayout hsDefault_m = new HotSpotLayouter(model, false, false, false).layout(cd);
                        ClassLayout hsHier_m = new HotSpotLayouter(model, true, false, false).layout(cd);
                        ClassLayout hsSuper_m = new HotSpotLayouter(model, true, true, false).layout(cd);

                        ClassLayout hsDefault_a = new HotSpotLayouter(model, false, false, true).layout(cd);
                        ClassLayout hsHier_a = new HotSpotLayouter(model, true, false, true).layout(cd);
                        ClassLayout hsSuper_a = new HotSpotLayouter(model, true, true, true).layout(cd);

                        System.out.printf("%3d, %3d, %3d, %3d, %3d, %3d, %3d, %3d, %3d, %s%n",
                                model.headerSize(),
                                raw.instanceSize(),
                                vm.instanceSize(),
                                hsDefault_m.instanceSize(),
                                hsHier_m.instanceSize(),
                                hsSuper_m.instanceSize(),
                                hsDefault_a.instanceSize(),
                                hsHier_a.instanceSize(),
                                hsSuper_a.instanceSize(),
                                klass.getName());
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
