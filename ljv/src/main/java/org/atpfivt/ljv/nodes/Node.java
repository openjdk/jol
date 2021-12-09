package org.atpfivt.ljv.nodes;

import org.atpfivt.ljv.Visualization;

import java.util.HashMap;

public abstract class Node {

    private final Object value;
    private String name;
    private String attributes;

    public Node(Object obj, String name) {
        this.value = obj;
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getAttributes(){
        return attributes;
    }

    abstract public void visit(Visualization v);
}
