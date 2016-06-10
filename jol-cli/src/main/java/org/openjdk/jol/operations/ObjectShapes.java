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
import org.openjdk.jol.heap.HeapDumpReader;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.util.Multiset;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Aleksey Shipilev
 */
public class ObjectShapes implements Operation {

    @Override
    public String label() {
        return "shapes";
    }

    @Override
    public String description() {
        return "Dump the object shapes present in JAR files or heap dumps.";
    }

    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.err.println("Expected one or more JAR/heapdump file names.");
            System.exit(1);
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        CompletionService<Multiset<String>> cs = new ExecutorCompletionService<Multiset<String>>(threadPool);

        for (final String arg : args) {
            if (arg.endsWith(".jar")) {
                cs.submit(new Callable<Multiset<String>>() {
                    @Override
                    public Multiset<String> call() throws Exception {
                        return processJAR(arg);
                    }
                });
            }
            if (arg.endsWith(".dump") || arg.endsWith("hprof") || arg.endsWith("hprof.gz")) {
                cs.submit(new Callable<Multiset<String>>() {
                    @Override
                    public Multiset<String> call() throws Exception {
                        return processHeapDump(arg);
                    }
                });
            }
        }

        Multiset<String> shapes = new Multiset<String>();
        for (String arg : args) {
            Multiset<String> ms = cs.take().get();
            shapes.merge(ms);
        }

        threadPool.shutdown();

        for (String key : shapes.keys()) {
            System.out.printf("%d\t%s%n", shapes.count(key), key);
        }
    }

    private Multiset<String> processHeapDump(String arg) {
        Multiset<String> shapes = new Multiset<String>();
        try {
            HeapDumpReader reader = new HeapDumpReader(new File(arg));
            Multiset<ClassData> data = reader.parse();
            for (ClassData cd : data.keys()) {
                String shape = parseClassData(cd);
                shapes.add(shape, data.count(cd));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shapes;
    }

    private Multiset<String> processJAR(String jarName) {
        Multiset<String> shapes = new Multiset<String>();
        try {
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
                    String shape = parseClassData(cd);
                    shapes.add(shape);
                } catch (Error t) {
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            jarFile.close();
        } catch (Exception t) {
            // ignore
        }
        return shapes;
    }

    private String parseClassData(ClassData cd) {
        StringBuilder sb = new StringBuilder();
        for (String cn : cd.classHierarchy()) {
            for (FieldData fd : cd.fieldsFor(cn)) {
                sb.append(toTypeLabel(fd.typeClass()));
            }
            sb.append("|");
        }
        if (sb.length() == 0) {
            sb.append("|");
        }
        if (sb.charAt(0) != '|') {
            sb.insert(0, '|');
        }
        return sb.toString();
    }

    private static char toTypeLabel(String s) {
        if ("boolean".equals(s))    return 'Z';
        if ("byte".equals(s))       return 'B';
        if ("char".equals(s))       return 'C';
        if ("short".equals(s))      return 'S';
        if ("int".equals(s))        return 'I';
        if ("float".equals(s))      return 'F';
        if ("long".equals(s))       return 'L';
        if ("double".equals(s))     return 'D';
        return 'L';
    }

}
