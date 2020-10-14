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
package org.openjdk.jol.info;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Basic class to walk object graphs.
 *
 * @author Aleksey Shipilev
 */
abstract class AbstractGraphWalker {

    private final Map<Class<?>, Field[]> fieldsCache;

    public AbstractGraphWalker() {
        fieldsCache = new HashMap<>();
    }

    protected void verifyRoots(Object... roots) {
        if (roots == null) {
            throw new IllegalArgumentException("Roots are null");
        }
        for (Object root : roots) {
            if (root == null) {
                throw new IllegalArgumentException("Some root is null");
            }
        }
    }

    protected Field[] getAllReferences(Class<?> klass) {
        Field[] exist = fieldsCache.get(klass);
        if (exist != null) {
            return exist;
        }

        List<Field> results = new ArrayList<>();

        for (Field f : klass.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (f.getType().isPrimitive()) continue;
            results.add(f);
        }

        Class<?> superKlass = klass;
        while ((superKlass = superKlass.getSuperclass()) != null) {
            for (Field f : superKlass.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType().isPrimitive()) continue;
                results.add(f);
            }
        }

        Field[] arr = results.toArray(new Field[0]);
        fieldsCache.put(klass, arr);

        return arr;
    }

}
