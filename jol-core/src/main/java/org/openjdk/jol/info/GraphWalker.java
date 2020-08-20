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

import org.openjdk.jol.util.ObjectUtils;
import org.openjdk.jol.vm.VM;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic class to walk object graphs.
 *
 * @author Aleksey Shipilev
 */
public class GraphWalker {

    private final Map<Class<?>, Field[]> fieldsCache;
    private final Map<Class<?>, Long> sizeCache;

    public GraphWalker() {
        fieldsCache = new HashMap<>();
        sizeCache = new HashMap<>();
    }

    public GraphLayout walk(Object... roots) {
        GraphLayout data = new GraphLayout(roots);

        IdentityHashSet visited = new IdentityHashSet(IdentityHashSet.MINIMUM_CAPACITY);
        ArrayDeque<GraphPathRecord> q = new ArrayDeque<>();

        int rootId = 1;
        boolean single = (roots.length == 1);
        for (Object root : roots) {
            String label = single ? "" : ("<r" + rootId + ">");
            GraphPathRecord e = new FieldGraphPathRecord(null, label, 0, root);
            if (visited.add(root)) {
                data.addRecord(e);
                q.add(e);
            }
            rootId++;
        }

        while (!q.isEmpty()) {
            GraphPathRecord cGpr = q.pop();
            Object o = cGpr.obj();
            Class<?> cl = o.getClass();

            if (cl.isArray()) {
                if (cl.getComponentType().isPrimitive()) {
                    // Nothing to do here
                    continue;
                }

                Object[] arr = (Object[]) o;

                for (int i = 0; i < arr.length; i++) {
                    Object e = arr[i];
                    if (e != null && visited.add(e)) {
                        GraphPathRecord gpr = new ArrayGraphPathRecord(cGpr, i, cGpr.depth() + 1, e);
                        data.addRecord(gpr);
                        q.push(gpr);
                    }
                }
            } else {
                Long knownSize = sizeCache.get(cl);
                if (knownSize == null) {
                    knownSize = VM.current().sizeOf(o);
                    sizeCache.put(cl, knownSize);
                }
                cGpr.setSize(knownSize);

                Field[] fields = getAllReferences(cl);
                for (Field f : fields) {
                    Object e = ObjectUtils.value(o, f);
                    if (e != null && visited.add(e)) {
                        GraphPathRecord gpr = new FieldGraphPathRecord(cGpr, f.getName(), cGpr.depth() + 1, e);
                        data.addRecord(gpr);
                        q.push(gpr);
                    }
                }
            }
        }

        return data;
    }

    private Field[] getAllReferences(Class<?> klass) {
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

    private static final class IdentityHashSet {
        private static final int MINIMUM_CAPACITY = 4;
        private static final int MAXIMUM_CAPACITY = 1 << 29;

        private Object[] table;
        private int size;

        public IdentityHashSet(int expectedMaxSize) {
            table = new Object[capacity(expectedMaxSize)];
        }

        private static int capacity(int expectedMaxSize) {
            return (expectedMaxSize > MAXIMUM_CAPACITY / 3) ? MAXIMUM_CAPACITY :
                   (expectedMaxSize <= 2 * MINIMUM_CAPACITY / 3) ? MINIMUM_CAPACITY :
                   Integer.highestOneBit(expectedMaxSize + (expectedMaxSize << 1));
        }

        private static int hash(Object x, int length) {
            return System.identityHashCode(x) & (length - 1);
        }

        private static int nextIndex(int i, int len) {
            return (i + 1 < len ? i + 1 : 0);
        }

        public boolean add(Object o) {
            while (true) {
                final Object[] tab = table;
                final int len = tab.length;
                int i = hash(o, len);

                for (Object item; (item = tab[i]) != null; i = nextIndex(i, len)) {
                    if (item == o) {
                        return false;
                    }
                }

                final int s = size + 1;
                if (s*3 > len && resize(len)) continue;

                tab[i] = o;
                size = s;
                return true;
            }
        }

        private boolean resize(int newCapacity) {
            int newLength = newCapacity * 2;

            Object[] oldTable = table;
            int oldLength = oldTable.length;
            if (oldLength == 2 * MAXIMUM_CAPACITY) { // can't expand any further
                if (size == MAXIMUM_CAPACITY - 1) {
                    throw new IllegalStateException("Capacity exhausted.");
                }
                return false;
            }
            if (oldLength >= newLength)
                return false;

            Object[] newTable = new Object[newLength];

            for (Object o : oldTable) {
                if (o != null) {
                    int i = hash(o, newLength);
                    while (newTable[i] != null) {
                        i = nextIndex(i, newLength);
                    }
                    newTable[i] = o;
                }
            }
            table = newTable;
            return true;
        }
    }
}
