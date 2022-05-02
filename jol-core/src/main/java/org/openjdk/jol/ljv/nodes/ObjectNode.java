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

import java.util.HashMap;
import java.util.List;

public class ObjectNode extends Node {

    private final String className;
    private final int primitiveFieldsNum;
    private List<Node> children;

    public ObjectNode(Object obj, String name, String className, int primitiveFieldsNum, List<Node> children,
                      String attributes) {
        super(obj, name);
        this.className = className;
        this.primitiveFieldsNum = primitiveFieldsNum;
        this.children = children;
        this.setAttributes(attributes);
    }

    public ObjectNode(ObjectNode node) {
        super(node.getValue(), node.getName());
        this.setAttributes(node.getAttributes());
        this.className = node.getClassName();
        this.primitiveFieldsNum = node.getPrimitiveFieldsNum();
        this.children = node.getChildren();
    }

    public String getClassName() {
        return className;
    }

    public int getPrimitiveFieldsNum() {
        return primitiveFieldsNum;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    @Override
    public void visit(Visualization v) {
        v.visitObjectBegin(this);
        // First processing only primitive fields
        for (Node node : children) {
            if (node instanceof PrimitiveNode) {
                node.visit(v);
            }
        }
        v.visitObjectEnd(getValue());
        // Next, processing non-primitive objects and making relations with them
        for (Node node : children) {
            if (node instanceof PrimitiveNode) {
                continue;
            }
            if (!v.alreadyVisualized(node.getValue())) {
                node.visit(v);
            }
            String attributes = node.getAttributes();
            if (attributes == null) attributes = "";
            v.visitObjectFieldRelationWithNonPrimitiveObject(getValue(), node, attributes);
        }
    }
}
