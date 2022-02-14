package org.atpfivt.ljv.nodes;

import org.atpfivt.ljv.Visualization;

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
