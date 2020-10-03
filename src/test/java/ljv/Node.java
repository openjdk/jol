package ljv;

class Node {
    String name;
    int level;
    boolean ok;
    Node left, right;

    public Node(String n, int l) {
        name = n;
        level = l;
        ok = l % 2 == 0;
    }

    public String toString() {
        return "";
    }
}
