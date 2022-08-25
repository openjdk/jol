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

import org.openjdk.jol.util.SimpleIdentityHashSet;
import org.openjdk.jol.util.ObjectUtils;
import org.openjdk.jol.util.SimpleStack;
import org.openjdk.jol.vm.VM;
import org.openjdk.jol.vm.VirtualMachine;

import java.lang.reflect.Field;

/**
 * Walker for graph statistics.
 *
 * @author Aleksey Shipilev
 */
public class GraphStatsWalker extends AbstractGraphWalker {

    public GraphStats walk(Object... roots) {
        verifyRoots(roots);

        GraphStats data = new GraphStats();

        SimpleIdentityHashSet visited = new SimpleIdentityHashSet();
        SimpleStack<Object> s = new SimpleStack<>();
        VirtualMachine vm = VM.current();

        for (Object root : roots) {
            if (visited.add(root)) {
                data.addRecord(vm.sizeOf(root));
                s.push(root);
            }
        }

        while (!s.isEmpty()) {
            Object o = s.pop();
            Class<?> cl = o.getClass();

            if (cl.isArray()) {
                if (cl.getComponentType().isPrimitive()) {
                    // Nothing to do here
                    continue;
                }

                for (Object e : (Object[]) o) {
                    if (e != null && visited.add(e)) {
                        data.addRecord(vm.sizeOf(e));
                        s.push(e);
                    }
                }
            } else {
                for (Field f : getAllReferenceFields(cl)) {
                    Object e = ObjectUtils.value(o, f);
                    if (e != null && visited.add(e)) {
                        data.addRecord(vm.sizeOf(e));
                        s.push(e);
                    }
                }
            }
        }

        return data;
    }

}
