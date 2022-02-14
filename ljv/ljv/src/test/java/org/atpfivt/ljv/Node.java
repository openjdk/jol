package org.atpfivt.ljv;

class Node {
    String name;
    int level;
    AnotherNode left, right;

    public Node(String n) {
        name = n;
    }

    public String toString() {
        return "";
    }
}

class AnotherNode {
    String name;
    int level;
    Node left, right;

    public AnotherNode(String n) {
        name = n;
    }

    public String toString() {
        return "";
    }
}
