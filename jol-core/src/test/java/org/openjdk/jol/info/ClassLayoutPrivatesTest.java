package org.openjdk.jol.info;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jol.TestUtils;

public class ClassLayoutPrivatesTest {

    @Before
    public void beforeMethod() {
        Assume.assumeTrue(TestUtils.JDK_VERSION < 25 || (System.getProperty("jol.magicFieldOffset") != null));
    }

    static class A {

    }

    static class B {
        private A a = new A();
    }

    @Test
    public void testPrint() {
        ClassLayout.parseInstance(new A()).toPrintable();
        ClassLayout.parseInstance(new B()).toPrintable();

        ClassLayout.parseClass(A.class).toPrintable();
        ClassLayout.parseClass(B.class).toPrintable();

        ClassLayout.parseClass(A.class).toPrintable(new A());
        ClassLayout.parseClass(B.class).toPrintable(new B());
    }

    @Test
    public void testClass() {
        GraphLayout.parseInstance(this.getClass()).toPrintable();
    }

}
