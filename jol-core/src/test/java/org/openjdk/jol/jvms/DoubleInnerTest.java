/*
 * Copyright (c) 2017, Red Hat Inc. All rights reserved.
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

package org.openjdk.jol.jvms;

import junit.framework.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.util.ByteClassLoader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.objectweb.asm.Opcodes.*;

public class DoubleInnerTest {

    private static final int CLASSFILE_VERSION = 50;

    public Collection<Class<?>> generate() throws ClassNotFoundException, IOException {
        Collection<Class<?>> res = new ArrayList<Class<?>>();
        ByteClassLoader classLoader = new ByteClassLoader();
        {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            cw.visit(CLASSFILE_VERSION,
                    ACC_PUBLIC + ACC_SUPER,
                    "Outer$Inner$SecondInner",
                    null,
                    Type.getInternalName(Object.class),
                    new String[0]);

            cw.visitInnerClass(
                    "Outer$Inner$",
                    "Outer",
                    "Inner$",
                    ACC_PUBLIC + ACC_STATIC
            );

            cw.visitInnerClass(
                    "Outer$Inner$MoreInner",
                    "Outer$Inner$",
                    "MoreInner",
                    ACC_PUBLIC + ACC_STATIC
            );

            cw.visitEnd();
            classLoader.put("Outer$Inner$SecondInner", cw.toByteArray());
            FileOutputStream fos = new FileOutputStream("out3.class");
            fos.write(cw.toByteArray());
            fos.close();
            res.add(classLoader.findClass("Outer$Inner$SecondInner"));
        }
        {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            cw.visit(CLASSFILE_VERSION,
                    ACC_PUBLIC + ACC_SUPER,
                    "Outer$Inner$",
                    null,
                    Type.getInternalName(Object.class),
                    new String[0]);

            cw.visitInnerClass(
                    "Outer$Inner$",
                    "Outer",
                    "Inner$",
                    ACC_PUBLIC + ACC_STATIC
            );

            cw.visitInnerClass(
                    "Outer$Inner$MoreInner",
                    "Outer$Inner$",
                    "MoreInner",
                    ACC_PUBLIC + ACC_STATIC
            );

            cw.visitEnd();
            classLoader.put("Outer$Inner$", cw.toByteArray());
            FileOutputStream fos = new FileOutputStream("out2.class");
            fos.write(cw.toByteArray());
            fos.close();
            res.add(classLoader.findClass("Outer$Inner$"));
        }
        {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            cw.visit(CLASSFILE_VERSION,
                    ACC_PUBLIC + ACC_FINAL + ACC_SUPER,
                    "Outer$",
                    null,
                    Type.getInternalName(Object.class),
                    new String[0]);

            cw.visitInnerClass(
                    "Outer$Inner$",
                    "Outer",
                    "Inner$",
                    ACC_PUBLIC + ACC_STATIC
            );

            cw.visitInnerClass(
                    "Outer$Inner$MoreInner",
                    "Outer$Inner$",
                    "MoreInner",
                    ACC_PUBLIC + ACC_STATIC
            );

            cw.visitEnd();
            classLoader.put("Outer$", cw.toByteArray());
            FileOutputStream fos = new FileOutputStream("out1.class");
            fos.write(cw.toByteArray());
            fos.close();

            res.add(classLoader.findClass("Outer$"));
        }
        return res;
    }

    @Test
    public void test() throws ClassNotFoundException, IOException {
        Collection<Class<?>> generate = generate();
        Assert.assertEquals(3, generate.size());

        for (Class<?> klass : generate) {
            ClassLayout cl = ClassLayout.parseClass(klass);
            Assert.assertNotNull(null, cl.toPrintable());
        }
    }

}
