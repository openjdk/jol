/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class ClassGenerator {

    private static final int CLASSFILE_VERSION = 50;

    private static final AtomicInteger idx = new AtomicInteger();

    public static Class<?> generate(Random r, int maxHierarchyDepth, int maxFieldsPerClass) throws Exception {
        ByteClassLoader classLoader = new ByteClassLoader();

        int numClasses = r.nextInt(maxHierarchyDepth + 1);
        Class<?> sup = Object.class;
        for (int c = 0; c < numClasses; c++) {
            sup = generate(r, sup, classLoader, maxFieldsPerClass);
        }
        return sup;
    }

    private static Class<?> generate(Random r, Class<?> superClass, ByteClassLoader classLoader, int maxFieldsPerClass) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        String name = "Class" + idx.incrementAndGet();

        cw.visit(CLASSFILE_VERSION,
                ACC_PUBLIC + ACC_SUPER,
                name,
                null,
                Type.getInternalName(superClass),
                new String[0]);

        cw.visitSource(name + ".java", null);

        Class<?>[] types = new Class[] { boolean.class, byte.class, short.class, char.class, int.class, float.class, long.class, double.class, Object.class };

        int count = r.nextInt(maxFieldsPerClass);
        for (int c = 0; c < count; c++) {
            Class<?> type = types[r.nextInt(types.length)];
            cw.visitField(ACC_PUBLIC, "field" + c, Type.getType(type).getDescriptor(), null, null);
        }

        cw.visitEnd();

        classLoader.put(name, cw.toByteArray());
        return classLoader.findClass(name);
    }

}
