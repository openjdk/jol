package org.atpfivt.ljv.nodes;

import org.atpfivt.ljv.Visualization;

public class PrimitiveNode extends Node {

    public PrimitiveNode(Object obj, String name) {
        super(obj, name);
    }

    @Override
    public void visit(Visualization v) {
        v.visitObjectPrimitiveField(getName(), String.valueOf(getValue()));
    }

}
