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
import org.openjdk.jol.util.SimpleIdentityHashSet;
import org.openjdk.jol.util.SimpleStack;
import org.openjdk.jol.vm.VM;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Concrete class to walk object graphs.
 *
 * @author Aleksey Shipilev
 */
public class GraphWalker extends AbstractGraphWalker {

    private final GraphVisitor[] visitors;
    private final HashMap<Class<?>, Long> sizeCache;

    public GraphWalker(GraphVisitor... visitor) {
        this.visitors = visitor;
        sizeCache = new HashMap<>();
    }

    public GraphLayout walk(Object... roots) {
        verifyRoots(roots);

        GraphLayout data = new GraphLayout(roots);

        SimpleIdentityHashSet visited = new SimpleIdentityHashSet();
        SimpleStack<GraphPathRecord> s = new SimpleStack<>();

        int rootId = 1;
        boolean single = (roots.length == 1);
        for (Object root : roots) {
            String label = single ? "" : ("<r" + rootId + ">");
            GraphPathRecord e = new FieldGraphPathRecord(null, label, 0, root);
            if (visited.add(root)) {
                data.addRecord(e);
                s.push(e);
            }
            rootId++;
        }

        while (!s.isEmpty()) {
            GraphPathRecord cGpr = s.pop();
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
                        for (GraphVisitor v : visitors) {
                            v.visit(gpr);
                        }
                        s.push(gpr);
                    }
                }
            } else {
                Long knownSize = sizeCache.get(cl);
                if (knownSize == null) {
                    knownSize = VM.current().sizeOf(o);
                    sizeCache.put(cl, knownSize);
                }
                cGpr.setSize(knownSize);

                for (Field f : getAllReferenceFields(cl)) {
                    Object e = ObjectUtils.value(o, f);
                    if (e != null && visited.add(e)) {
                        GraphPathRecord gpr = new FieldGraphPathRecord(cGpr, f.getName(), cGpr.depth() + 1, e);
                        data.addRecord(gpr);
                        for (GraphVisitor v : visitors) {
                            v.visit(gpr);
                        }
                        s.push(gpr);
                    }
                }
            }
        }

        return data;
    }

}
