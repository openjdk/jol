import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;

public class GraphLayoutTest {

    static class A {

    }

    static class B {
        A a = new A();
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

        int aSize_insta = ClassLayout.parseInstance(a).instanceSize();
        int bSize_insta = ClassLayout.parseInstance(b).instanceSize();

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

}
