package org.atpfivt.ljv;

import java.util.LinkedList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Integer> linkedList = new LinkedList<>();
        linkedList.add(1);
        linkedList.add(42);
        linkedList.add(21);
        System.out.println(
                new LJV()
                        .setTreatAsPrimitive(Integer.class)
                        .setDirection(Direction.LR)
                        .drawGraph(linkedList));
    }
}
