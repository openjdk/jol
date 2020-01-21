package org.openjdk.jol.info;

import org.junit.Assert;
import org.junit.Test;

public class GraphLayoutMultipleRootTest {

    @Test
    public void test() {
        Object r = new Object();
        GraphLayout gl1 = GraphLayout.parseInstance(r);
        GraphLayout gl2 = GraphLayout.parseInstance(r, r);
        GraphLayout gl3 = GraphLayout.parseInstance(r, r, r);

        Assert.assertEquals(gl1.totalCount(), gl2.totalCount());
        Assert.assertEquals(gl1.totalCount(), gl3.totalCount());
        Assert.assertEquals(gl1.totalSize(),  gl2.totalSize());
        Assert.assertEquals(gl1.totalSize(),  gl3.totalSize());
    }

}
