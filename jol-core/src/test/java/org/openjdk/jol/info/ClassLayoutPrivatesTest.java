package org.openjdk.jol.info;

import org.junit.Test;

public class ClassLayoutPrivatesTest {

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
