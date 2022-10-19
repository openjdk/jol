package org.openjdk.jol.info;

import org.junit.Assert;
import org.junit.Test;

public class GraphStatsTest {

    static class A {

    }

    static class B {
        A a = new A();
    }

    static class C {
        private final A a;
        public C(A a) { this.a = a; }
    }

    static class D {
        private final D d;
        public D(D d) { this.d = d; }
    }

    static class E {
        private final A[] as;

        E() {
            as = new A[] { new A() };
        }
    }

    @Test
    public void basicCounts() {
        A a = new A();
        B b = new B();

        Assert.assertEquals("Reports one instance only: A",
                1, GraphStats.parseInstance(a).totalCount());

        Assert.assertEquals("Reports two instances: B and B.a",
                2, GraphStats.parseInstance(b).totalCount());

        Assert.assertEquals("Reports three instances: A, B, and B.a",
                3, GraphStats.parseInstance(a, b).totalCount());

        Assert.assertEquals("Reports two instances: B and B.a",
                2, GraphStats.parseInstance(b.a, b).totalCount());
    }

    @Test
    public void basicSizes() {
        A a = new A();
        B b = new B();

        long aSize = GraphStats.parseInstance(a).totalSize();
        long bSize = GraphStats.parseInstance(b).totalSize();

        long aSize_insta = ClassLayout.parseInstance(a).instanceSize();
        long bSize_insta = ClassLayout.parseInstance(b).instanceSize();

        Assert.assertEquals("GraphStats and ClassLayout sizes agree on A",
                aSize,
                aSize_insta);

        Assert.assertNotEquals("GraphStats and ClassLayout sizes disagree on B",
                bSize,
                bSize_insta);

        Assert.assertEquals("GraphStats size for B = ClassLayout size for A + ClassLayout size for B",
                bSize,
                aSize_insta + bSize_insta);

        Assert.assertEquals("GraphStats size of B = sum of all sizes",
                GraphStats.parseInstance(b).totalSize(),
                GraphStats.parseInstance(b.a, b).totalSize());
    }

    @Test
    public void compoundSizes() {
        for (int s : new int[] {0, 1, 10, 100, 1000}) {
            D d = new D(null);
            for (int i = 0; i < s; i++) {
                d = new D(d);
            }

            long dSize = GraphStats.parseInstance(d).totalSize();
            long dSize_insta = ClassLayout.parseInstance(d).instanceSize();

            Assert.assertEquals("GraphStats size of D chain = size of D times " + (s + 1),
                    dSize, dSize_insta * (s+1));
        }
    }

    @Test
    public void layoutAndStatsMatch() {
        E e = new E();

        GraphLayout layout = GraphLayout.parseInstance(e);
        GraphStats stats = GraphStats.parseInstance(e);

        Assert.assertEquals(layout.totalSize(), stats.totalSize());
        Assert.assertEquals(layout.totalCount(), stats.totalCount());
    }
}
