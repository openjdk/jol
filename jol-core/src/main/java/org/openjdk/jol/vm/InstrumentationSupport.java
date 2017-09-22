/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jol.vm;

import org.openjdk.jol.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Instrumentation API support.
 *
 * @author Aleksey Shipilev (original code, refactoring)
 * @author Rafael Winterhalter (dynamic attach)
 */
class InstrumentationSupport {

    private static Instrumentation INSTRUMENTATION;

    private static boolean TRY_DYNAMIC_ATTACH = !Boolean.getBoolean("jol.skipDynamicAttach");

    public static void premain(String agentArgs, Instrumentation inst) {
        INSTRUMENTATION = inst;
    }

    static Instrumentation instance() {
        if (INSTRUMENTATION != null) return INSTRUMENTATION;

        if (TRY_DYNAMIC_ATTACH) {
            try {
                tryDynamicAttach();
                return INSTRUMENTATION;
            } catch (Exception ignored) {
                throw new InstrumentationException("Dynamic Attach failed. You may add this JAR as -javaagent manually, or supply -Djdk.attach.allowAttachSelf");
            }
        }
        throw new InstrumentationException("No instrumentation. Add this JAR as -javaagent manually.");
    }

    private static void tryDynamicAttach() throws Exception {
        Class<?> vmClass;

        String name = "com.sun.tools.attach.VirtualMachine";
        try {
            // JDK 9+ makes this class available on class path
            vmClass = ClassLoader.getSystemClassLoader().loadClass(name);
        } catch (Exception e) {
            // JDK -8 ships this class in tools.jar
            URL url = new File(System.getProperty("java.home")
                    .replace('\\', '/') + "/../lib/tools.jar").toURI().toURL();
            ClassLoader classLoader = new URLClassLoader(new URL[]{url}, null);
            vmClass = classLoader.loadClass(name);
        }

        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        Object vm = vmClass.getDeclaredMethod("attach", String.class)
                .invoke(null, runtimeName.substring(0, runtimeName.indexOf('@')));
        try {
            File agentFile = File.createTempFile("jolAgent", ".jar");
            try {
                saveAgentJar(agentFile);
                vmClass.getDeclaredMethod("loadAgent", String.class, String.class)
                        .invoke(vm, agentFile.getAbsolutePath(), "");
                Field field = ClassLoader.getSystemClassLoader()
                            .loadClass(Installer.class.getName())
                            .getDeclaredField("INSTRUMENTATION");
                INSTRUMENTATION = (Instrumentation) field.get(null);
            } finally {
                agentFile.delete();
            }
        } finally {
            vmClass.getDeclaredMethod("detach").invoke(vm);
        }
    }

    private static void saveAgentJar(File agentFile) throws Exception {
        InputStream is = Installer.class.getResourceAsStream('/' + Installer.class.getName().replace('.', '/') + ".class");
        if (is == null) {
            return;
        }
        try {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(new Attributes.Name("Agent-Class"), Installer.class.getName());
            JarOutputStream jos = new JarOutputStream(new FileOutputStream(agentFile), manifest);
            try {
                jos.putNextEntry(new JarEntry(Installer.class.getName().replace('.', '/') + ".class"));
                byte[] buffer = new byte[1024];
                int index;
                while ((index = is.read(buffer)) != -1) {
                    jos.write(buffer, 0, index);
                }
                jos.closeEntry();
            } finally {
                IOUtils.safelyClose(jos);
            }
        } finally {
            IOUtils.safelyClose(is);
        }
    }

    private static class Installer {
        public static volatile Instrumentation INSTRUMENTATION;
        public static void agentmain(String agentArgs, Instrumentation inst) {
            INSTRUMENTATION = inst;
        }
    }
}
