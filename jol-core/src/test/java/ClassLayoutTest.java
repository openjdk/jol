import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.datamodel.CurrentDataModel;
import org.openjdk.jol.info.ClassLayout;

import java.lang.reflect.Array;

public class ClassLayoutTest {

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

    static final int SIZE_SLACK = 16;

    @Test
    public void arraySizes_0() {
        for (Class<?> cl : TYPES) {
            Object o = Array.newInstance(cl, 0);
            ClassLayout l = ClassLayout.parseInstance(o);
            Assert.assertEquals(cl + "[0] size equals to header size",
                    l.headerSize(),
                    l.instanceSize());
        }
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

            int elementSize = new CurrentDataModel().sizeOf(cl.getName());
            long expected = l.headerSize() + (long) size * elementSize;
            long actual = l.instanceSize();

            Assert.assertTrue("Array instance size is not within range: actual = " + actual + ", expected = " + expected,
                    (actual - SIZE_SLACK <= expected) && (expected <= actual + SIZE_SLACK));

            if (l.instanceSize() <= 0) {
                Assert.fail(cl + "[" + size + "] is not positive: " + l.instanceSize());
            }
        }
    }

}
