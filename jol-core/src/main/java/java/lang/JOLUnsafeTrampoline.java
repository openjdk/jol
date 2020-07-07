/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
package java.lang;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class is needed to work around the protection that disallows polling objectFieldOffset
 * on Records and hidden classes:
 *   https://bugs.openjdk.java.net/browse/JDK-8247444
 *   https://hg.openjdk.java.net/jdk/jdk/file/ae002489df78/src/jdk.unsupported/share/classes/sun/misc/Unsafe.java#l640
 *
 * It does so by injecting this class with elevated protection levels with sun.misc.Unsafe.defineAnonymousClass,
 * and accessing the jdk.internal.misc.Unsafe.objectFieldOffset that does not have
 * this protection. The class would be injected with java.lang.Object privileges, which
 * also breaks through the module system protections.
 *
 * Since j.i.m.Unsafe is not accessible on every JDK, this code falls back gracefully when
 * it is not available. Since we want to be compilable on lower JDKs (for example 8), we
 * have to do reflective invocations as well.
 *
 * This file is normally compiled to bytecode for easier injection. We might have constructed
 * it at runtime with bytecode manipulation tools.
 *
 * The whole thing is slow, because it requires going through reflection a few times, but it
 * should be fine for introspection uses in JOL.
 */
public class JOLUnsafeTrampoline {

    static volatile Object unsafe;
    static volatile Method trampoline;
    static volatile RuntimeException failed;

    public static long objectFieldOffset(Field f) {
        if (failed != null) {
            throw failed;
        }

        if (trampoline != null) {
            try {
                return (long) trampoline.invoke(unsafe, f);
            } catch (IllegalAccessException | InvocationTargetException e) {
                failed = new IllegalStateException(e);
                throw failed;
            }
        }

        try {
            Class<?> cl = Class.forName("jdk.internal.misc.Unsafe");
            Method m = cl.getMethod("getUnsafe");
            unsafe = m.invoke(null);
            trampoline = unsafe.getClass().getMethod("objectFieldOffset", Field.class);
            return (long) trampoline.invoke(unsafe, f);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            failed = new IllegalStateException(e);
            throw failed;
        }
    }

}
