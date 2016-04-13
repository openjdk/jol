package org.openjdk.jol.info;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

import java.lang.reflect.Array;

public class ClassLayoutInstanceTest {

    static class A {

    }

    static class B {
        A a = new A();
    }

    static class C extends B {

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
    public void testInconsistentTypes() {
        try {
            ClassLayout.parseClass(A.class).toPrintable(new B());
            Assert.fail("Trying to pass instance B over the class A succeeded");
        } catch (Exception e) {
            // expected
        }

        try {
            ClassLayout.parseClass(B.class).toPrintable(new A());
            Assert.fail("Trying to pass instance A over the class B succeeded");
        } catch (Exception e) {
            // expected
        }

        try {
            ClassLayout.parseClass(B.class).toPrintable(new C());
        } catch (Exception e) {
            Assert.fail("Trying to pass instance C over the class B failed");
        }
    }

    @Test
    public void headers() {
        ClassLayout clA = ClassLayout.parseInstance(new A());
        ClassLayout clB = ClassLayout.parseInstance(new B());

        Assert.assertTrue("A header size is positive",
                clA.headerSize() >= 0);

        Assert.assertTrue("B header size is positive",
                clB.headerSize() >= 0);

        Assert.assertEquals("A and B header sizes agree",
                clA.headerSize(), clB.headerSize());

        Assert.assertTrue("A instance size agrees with A header size",
                clA.instanceSize() >= clA.headerSize());

        Assert.assertTrue("B instance size agrees with B header size",
                clB.instanceSize() >= clB.headerSize());

        Assert.assertTrue("B instance size is larger or equal A instance size",
                clB.instanceSize() >= clA.headerSize());
    }

}
