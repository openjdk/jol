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
package org.openjdk.jol.vm;

import org.openjdk.jol.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ContendedSupport {

    private static final Class<? extends Annotation> SUN_MISC_CONTENDED;
    private static final Class<? extends Annotation> JDK_INTERNAL_CONTENDED;

    static {
        Class<? extends Annotation> smContended;
        try {
            smContended = (Class<? extends Annotation>) ClassUtils.loadSystemClass("sun.misc.Contended");
        } catch (ClassNotFoundException e) {
            smContended = null;
        }
        SUN_MISC_CONTENDED = smContended;

        Class<? extends Annotation> intContended;
        try {
            intContended = (Class<? extends Annotation>) ClassUtils.loadSystemClass("jdk.internal.vm.annotation.Contended");
        } catch (ClassNotFoundException e) {
            intContended = null;
        }
        JDK_INTERNAL_CONTENDED = intContended;
    }

    public static boolean isContended(AnnotatedElement el) {
        if (getSunMiscContended(el) != null) {
            return true;
        }
        if (getJdkInternalContended(el) != null) {
            return true;
        }
        return false;
    }

    public static String contendedGroup(AnnotatedElement el) {
        Object smAnn = getSunMiscContended(el);
        if (smAnn != null) {
            return pullValue(SUN_MISC_CONTENDED, smAnn);
        }
        Object intAnn = getJdkInternalContended(el);
        if (intAnn != null) {
            return pullValue(JDK_INTERNAL_CONTENDED, intAnn);
        }
        return null;
    }

    private static Object getSunMiscContended(AnnotatedElement el) {
        if (SUN_MISC_CONTENDED == null) {
            return null;
        }
        return el.getAnnotation(SUN_MISC_CONTENDED);
    }

    private static Object getJdkInternalContended(AnnotatedElement el) {
        if (JDK_INTERNAL_CONTENDED == null) {
            return null;
        }
        return el.getAnnotation(JDK_INTERNAL_CONTENDED);
    }

    private static String pullValue(Class<? extends Annotation> klass, Object ann) {
        try {
            Method meth = klass.getMethod("value");
            return (String) meth.invoke(ann);
        } catch (NoSuchMethodException e) {
            printErrorOnce(e);
        } catch (InvocationTargetException e) {
            printErrorOnce(e);
        } catch (IllegalAccessException e) {
            printErrorOnce(e);
        }
        return "";
    }

    static volatile boolean shown;

    static void printErrorOnce(Throwable err) {
        if (shown) return;
        shown = true;
        System.out.println("Error while accessing @Contended value: " + err.getMessage());
        System.out.println();
    }

}
