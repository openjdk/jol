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
package org.openjdk.jol.ljv.nodes;

import org.openjdk.jol.ljv.Visualization;

import java.lang.reflect.Array;
import java.util.List;

public class ArrayNode extends Node {

    private final boolean valuesArePrimitive;
    private final List<Node> content;

    public ArrayNode(Object obj, String name, boolean valuesArePrimitive, List<Node> content) {
        super(obj, name);
        this.valuesArePrimitive = valuesArePrimitive;
        this.content = content;
    }

    public boolean areValuesPrimitive() {
        return valuesArePrimitive;
    }

    @Override
    public void visit(Visualization v) {
        int len = Array.getLength(getValue());
        v.visitArrayBegin(this);
        for (int i = 0; i < len; ++i) {
            Object element = Array.get(getValue(), i);
            v.visitArrayElement(this, String.valueOf(element), i);
        }
        v.visitArrayEnd(getValue());
        if (valuesArePrimitive) return;
        // Generating DOTs for array object elements and creating connection
        for (int i = 0; i < len; ++i) {
            Node node = content.get(i);
            if (node instanceof NullNode) {
                continue;
            }
            if (!v.alreadyVisualized(node.getValue())) {
                node.visit(v);
            }
            v.visitArrayElementObjectConnection(getValue(), i, node.getValue());
        }
    }
}
