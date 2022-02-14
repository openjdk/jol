package org.atpfivt.ljv.nodes;

import org.atpfivt.ljv.Visualization;

public class NullNode extends Node {

    public NullNode(Object obj, String name) {
        super(obj, name);
    }

    @Override
    public void visit(Visualization v) {
        v.visitNull();
    }
}
