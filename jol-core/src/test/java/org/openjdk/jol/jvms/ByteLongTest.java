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
import org.openjdk.jol.info.FieldLayout;
import org.openjdk.jol.util.ByteClassLoader;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;

public class ByteLongTest {

    private static final int CLASSFILE_VERSION = 50;

    public Class<?> generate() throws ClassNotFoundException {
        ByteClassLoader classLoader = new ByteClassLoader();
        {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            cw.visit(CLASSFILE_VERSION,
                    ACC_PUBLIC + ACC_SUPER,
                    "foo/byte",
                    null,
                    Type.getInternalName(Object.class),
                    new String[0]);

            cw.visitEnd();
            classLoader.put("foo.byte", cw.toByteArray());
        }
        {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            cw.visit(CLASSFILE_VERSION,
                    ACC_PUBLIC + ACC_SUPER,
                    "foo/long",
                    null,
                    Type.getInternalName(Object.class),
                    new String[0]);
            cw.visitEnd();
            classLoader.put("foo.long", cw.toByteArray());
        }
        {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            cw.visit(CLASSFILE_VERSION,
                    ACC_PUBLIC + ACC_SUPER,
                    "foo/bar",
                    null,
                    Type.getInternalName(Object.class),
                    new String[0]);

            cw.visitField(ACC_PUBLIC, "field1", "Lfoo/byte;", null, null);
            cw.visitField(ACC_PUBLIC, "field2", "Lfoo/long;", null, null);
            cw.visitField(ACC_PUBLIC, "field3", "Lfoo/byte;", null, null);
            cw.visitField(ACC_PUBLIC, "field4", "Lfoo/long;", null, null);
            cw.visitEnd();
            classLoader.put("foo.bar", cw.toByteArray());
        }
        return classLoader.findClass("foo.bar");
    }

    @Test
    public void test() throws ClassNotFoundException {
        Class<?> klass = generate();
        ClassLayout cl = ClassLayout.parseClass(klass);
        Assert.assertEquals(4, cl.fields().size());
        Assert.assertEquals("field1", cl.fields().first().name());
        Assert.assertEquals("field4", cl.fields().last().name());

        for (FieldLayout fl : cl.fields()) {
            Assert.assertTrue(fl.typeClass().equals("foo.byte") || fl.typeClass().equals("foo.long"));
        }

        Assert.assertNotNull(cl.toPrintable());
    }

}
