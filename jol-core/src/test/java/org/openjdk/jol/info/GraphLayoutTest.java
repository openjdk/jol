package org.openjdk.jol.info;

import org.junit.Assert;
import org.junit.Test;

public class GraphLayoutTest {

    static class A {

    }

    static class B {
        A a = new A();
    }

    static class C {
        private final A a;
        public C(A a) { this.a = a; }
    }

    @Test
    public void basicCounts() {
        A a = new A();
        B b = new B();

        Assert.assertEquals("Reports one instance only: A",
                1, GraphLayout.parseInstance(a).totalCount());

        Assert.assertEquals("Reports two instances: B and B.a",
                2, GraphLayout.parseInstance(b).totalCount());

        Assert.assertEquals("Reports three instances: A, B, and B.a",
                3, GraphLayout.parseInstance(a, b).totalCount());

        Assert.assertEquals("Reports two instances: B and B.a",
                2, GraphLayout.parseInstance(b.a, b).totalCount());
    }

    @Test
    public void basicSizes() {
        A a = new A();
        B b = new B();

        long aSize = GraphLayout.parseInstance(a).totalSize();
        long bSize = GraphLayout.parseInstance(b).totalSize();

        long aSize_insta = ClassLayout.parseInstance(a).instanceSize();
        long bSize_insta = ClassLayout.parseInstance(b).instanceSize();

        Assert.assertEquals("GraphLayout and ClassLayout sizes agree on A",
                aSize,
                aSize_insta);

        Assert.assertNotSame("GraphLayout and ClassLayout sizes disagree on B",
                bSize,
                bSize_insta);

        Assert.assertEquals("GraphLayout size for B = ClassLayout size for A + ClassLayout size for B",
                bSize,
                aSize_insta + bSize_insta);

        Assert.assertEquals("GraphLayout size of B = sum of all sizes",
                GraphLayout.parseInstance(b).totalSize(),
                GraphLayout.parseInstance(b.a, b).totalSize());
    }

    @Test
    public void add() {
        A a = new A();
        B b = new B();

        GraphLayout ga = GraphLayout.parseInstance(a);
        GraphLayout gb = GraphLayout.parseInstance(b);

        Assert.assertEquals("starting count(A) = 1",
                1, ga.totalCount());
        Assert.assertEquals("starting count(B) = 2",
                2, gb.totalCount());

        GraphLayout sum = ga.add(gb);

        Assert.assertEquals("count(A) = 1",
                1, ga.totalCount());
        Assert.assertEquals("count(B) = 2",
                2, gb.totalCount());
        Assert.assertEquals("count(A) + count(B) = count(A+B)",
                ga.totalCount() + gb.totalCount(),
                sum.totalCount());

        Assert.assertEquals("size(A) + size(B) = size(A+B)",
                ga.totalSize() + gb.totalSize(),
                sum.totalSize());
    }

    @Test
    public void subtract() {
        A a = new A();
        C c = new C(a);

        GraphLayout ga = GraphLayout.parseInstance(a);
        GraphLayout gc = GraphLayout.parseInstance(c);

        Assert.assertEquals("starting count(A) = 1",
                1, ga.totalCount());
        Assert.assertEquals("starting count(C) = 2",
                2, gc.totalCount());

        GraphLayout diff = gc.subtract(ga);

        Assert.assertEquals("count(A) = 1",
                1, ga.totalCount());
        Assert.assertEquals("count(C) = 2",
                2, gc.totalCount());
        Assert.assertEquals("count(C) - count(A) = count(C-A)",
                gc.totalCount() - ga.totalCount(),
                diff.totalCount());

        Assert.assertEquals("size(C) - size(A) = size(C-A)",
                gc.totalSize() - ga.totalSize(),
                diff.totalSize());
    }

}
