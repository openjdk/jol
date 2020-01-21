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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Basic class to walk object graphs.
 *
 * @author Aleksey Shipilev
 */
public class GraphWalker {

    private final Set<Object> visited;
    private final Object[] roots;
    private final Collection<GraphVisitor> visitors;
    private final Map<Class<?>, Field[]> fieldsCache;

    public GraphWalker(Object... roots) {
        this.roots = roots;
        this.visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        this.visitors = new ArrayList<>();
        this.fieldsCache = new HashMap<>();
    }

    public void addVisitor(GraphVisitor v) {
        visitors.add(v);
    }

    public void walk() {
        List<GraphPathRecord> curLayer = new ArrayList<>();
        List<GraphPathRecord> newLayer = new ArrayList<>();

        int rootId = 1;
        boolean single = (roots.length == 1);
        for (Object root : roots) {
            String label = single ? "" : ("<r" + rootId + ">");
            GraphPathRecord e = new FieldGraphPathRecord(null, label, 0, root);
            if (visited.add(root)) {
                visitObject(e);
                curLayer.add(e);
            }
            rootId++;
        }

        while (!curLayer.isEmpty()) {
            newLayer.clear();
            for (GraphPathRecord next : curLayer) {
                Object o = next.obj();

                if (o.getClass().isArray()) {
                    if (o.getClass().getComponentType().isPrimitive()) {
                        // Nothing to do here
                        continue;
                    }

                    Object[] arr = (Object[]) o;

                    for (int i = 0; i < arr.length; i++) {
                        Object e = arr[i];
                        if (e != null) {
                            if (visited.add(e)) {
                                GraphPathRecord gpr = new ArrayGraphPathRecord(next, i, next.depth() + 1, e);
                                visitObject(gpr);
                                newLayer.add(gpr);
                            }
                        }
                    }
                } else {
                    Field[] fields = getAllReferences(o.getClass());
                    for (Field f : fields) {
                        Object e = ObjectUtils.value(o, f);
                        if (e != null) {
                            if (visited.add(e)) {
                                GraphPathRecord gpr = new FieldGraphPathRecord(next, f.getName(), next.depth() + 1, e);
                                visitObject(gpr);
                                newLayer.add(gpr);
                            }
                        }
                    }
                }
            }
            curLayer.clear();
            curLayer.addAll(newLayer);
        }
    }

    private void visitObject(GraphPathRecord record) {
        for (GraphVisitor v : visitors) {
            v.visit(record);
        }
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

}
