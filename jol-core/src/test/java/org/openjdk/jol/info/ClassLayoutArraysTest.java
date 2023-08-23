package org.openjdk.jol.info;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

import java.lang.reflect.Array;

public class ClassLayoutArraysTest {

    static final Class<?>[] TYPES = new Class<?>[]{
            boolean.class,
            byte.class,
            short.class,
            char.class,
            int.class,
            float.class,
            long.class,
            double.class,
            Object.class,
    };

    @Test
    public void arraySizes_0() {
        doArraySizeFor(0);
    }

    @Test
    public void arraySizes_10000() {
        doArraySizeFor(10000);
    }

    @Test
    public void arraySizes_halfMax() {
        doArraySizeFor(Integer.MAX_VALUE / 2 - 10);
    }

    @Test
    public void arraySizes_max() {
        doArraySizeFor(Integer.MAX_VALUE - 10);
    }

    private void doArraySizeFor(int size) {
        for (Class<?> cl : TYPES) {
            Object o;
            try {
                o = Array.newInstance(cl, size);
            } catch (OutOfMemoryError e) {
                // Oh well, moving on.
                continue;
            }
            ClassLayout l = ClassLayout.parseInstance(o);

            int elementSize = VM.current().arrayIndexScale(cl.getName());

            // At least the header size, plus the element storage.
            // At most the alignment tail and the array base alignment.
            long expectedMin = l.headerSize() + (long) size * elementSize;
            long expectedMax = expectedMin + VM.current().objectAlignment() + 4;

            long actual = l.instanceSize();

            Assert.assertTrue(cl + " array instance size is not within range: actual = " + actual +
                            ", expected range = [" + expectedMin + ", " + expectedMax + "]",
                    (actual >= expectedMin && actual <= expectedMax));

            if (l.instanceSize() <= 0) {
                Assert.fail(cl + "[" + size + "] is not positive: " + l.instanceSize());
            }
        }
    }

}
