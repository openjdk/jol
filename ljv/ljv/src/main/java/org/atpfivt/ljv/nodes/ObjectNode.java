package org.atpfivt.ljv.nodes;

import org.atpfivt.ljv.Visualization;

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
        for (Node node: children) {
            if (node instanceof PrimitiveNode) {
                node.visit(v);
            }
        }
        v.visitObjectEnd(getValue());
        // Next, processing non-primitive objects and making relations with them
        for (Node node: children) {
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
