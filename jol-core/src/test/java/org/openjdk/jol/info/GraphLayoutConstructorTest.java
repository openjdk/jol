package org.openjdk.jol.info;

import org.junit.Assert;
import org.junit.Test;

public class GraphLayoutConstructorTest {

    static class A {

    }

    @Test
    public void varargs() {
        Assert.assertEquals("Reports one A",
                1, GraphLayout.parseInstance(new A()).totalCount());

        Assert.assertEquals("Reports two A",
                2, GraphLayout.parseInstance(new A(), new A()).totalCount());

        Assert.assertEquals("Reports single A", // varargs unfolds
                1, GraphLayout.parseInstance(new A[]{ new A() }).totalCount());

        Assert.assertEquals("Reports array and A", // explicitly avoiding varargs
                2, GraphLayout.parseInstance((Object)new A[]{ new A() }).totalCount());

        Assert.assertEquals("Reports one array and two A-s",
                3, GraphLayout.parseInstance(new A[]{ new A() }, new A()).totalCount());

        Assert.assertEquals("Reports two arrays and one A",
                3, GraphLayout.parseInstance(new A[]{ new A() }, new A[]{ }).totalCount());

        Assert.assertEquals("Reports two arrays and two A-s",
                4, GraphLayout.parseInstance(new A[]{ new A() }, new A[]{ new A() }).totalCount());
    }

}
