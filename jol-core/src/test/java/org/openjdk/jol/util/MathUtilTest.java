package org.openjdk.jol.util;

import org.junit.Assert;
import org.junit.Test;

public class MathUtilTest {

    static int slowIntAlign(int v, int a) {
        if (v % a == 0) {
            return v;
        } else {
            return ((v / a) + 1) * a;
        }
    }

    static long slowLongAlign(long v, int a) {
        if (v % a == 0) {
            return v;
        } else {
            return ((v / a) + 1) * a;
        }
    }

    @Test
    public void alignIntTest() {
        Assert.assertEquals(0, MathUtil.align(0, 2));
        Assert.assertEquals(2, MathUtil.align(1, 2));
        Assert.assertEquals(2, MathUtil.align(2, 2));
        Assert.assertEquals(4, MathUtil.align(3, 2));
        Assert.assertEquals(4, MathUtil.align(4, 2));

        for (int v = 0; v < 10; v++) {
            for (int a = 1; a <= 256; a *= 2) {
                Assert.assertEquals(slowIntAlign(v, a), MathUtil.align(v, a));
            }
        }
    }

    @Test
    public void alignLongTest() {
        Assert.assertEquals(0, MathUtil.align(0L, 2));
        Assert.assertEquals(2, MathUtil.align(1L, 2));
        Assert.assertEquals(2, MathUtil.align(2L, 2));
        Assert.assertEquals(4, MathUtil.align(3L, 2));
        Assert.assertEquals(4, MathUtil.align(4L, 2));

        for (long v = 0; v < 10; v++) {
            for (int a = 1; a <= 256; a *= 2) {
                Assert.assertEquals(slowLongAlign(v, a), MathUtil.align(v, a));
            }
        }

        for (long v = Integer.MAX_VALUE; v < Integer.MAX_VALUE + 10L; v++) {
            for (int a = 1; a <= 256; a *= 2) {
                Assert.assertEquals(slowLongAlign(v, a), MathUtil.align(v, a));
            }
        }
    }

}
