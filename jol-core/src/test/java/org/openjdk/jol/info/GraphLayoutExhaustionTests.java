package org.openjdk.jol.info;

import org.junit.Test;

import java.util.LinkedList;

public class GraphLayoutExhaustionTests {

    @Test
    public void testLinkedList() throws Exception {
        LinkedList<Integer> list = new LinkedList<Integer>();
        for (int i = 0; i < 1000000; i++) {
            list.add(i);
        }
        System.out.println(GraphLayout.parseInstance(list).toFootprint());
    }

}
