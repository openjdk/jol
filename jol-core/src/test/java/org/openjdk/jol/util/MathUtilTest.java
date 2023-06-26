package org.openjdk.jol.util;

import org.junit.Assert;
import org.junit.Test;

import java.text.NumberFormat;

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

    @Test
    public void inProperUnitsTest() {
        Assert.assertEquals("0", MathUtil.inProperUnits(0));
        Assert.assertEquals("1", MathUtil.inProperUnits(1));
        Assert.assertEquals("10", MathUtil.inProperUnits(10));
        Assert.assertEquals("100", MathUtil.inProperUnits(100));
        Assert.assertEquals("1000", MathUtil.inProperUnits(1000));
        Assert.assertEquals("10000", MathUtil.inProperUnits(10_000));
        Assert.assertEquals("100000", MathUtil.inProperUnits(100_000));

        Assert.assertEquals("100K", MathUtil.inProperUnits(100_000 + 1));
        Assert.assertEquals("1000K", MathUtil.inProperUnits(1_000_000));
        Assert.assertEquals("10000K", MathUtil.inProperUnits(10_000_000));
        Assert.assertEquals("100000K", MathUtil.inProperUnits(100_000_000));

        Assert.assertEquals("100M", MathUtil.inProperUnits(100_000_000 + 1));
        Assert.assertEquals("1000M", MathUtil.inProperUnits(1_000_000_000));
        Assert.assertEquals("10000M", MathUtil.inProperUnits(10_000_000_000L));
        Assert.assertEquals("100000M", MathUtil.inProperUnits(100_000_000_000L));

        Assert.assertEquals("100G", MathUtil.inProperUnits(100_000_000_000L + 1));
        Assert.assertEquals("1000G", MathUtil.inProperUnits(1_000_000_000_000L));
        Assert.assertEquals("10000G", MathUtil.inProperUnits(10_000_000_000_000L));
        Assert.assertEquals("100000G", MathUtil.inProperUnits(100_000_000_000_000L));
    }

    @Test
    public void diffInPercentTest() {
        Assert.assertEquals("0%", MathUtil.diffPercent(0, 0));
        Assert.assertEquals("0%", MathUtil.diffPercent(1, 1));

        Assert.assertEquals("~0%",   MathUtil.diffPercent(10001, 10000));
        Assert.assertEquals("~0%",   MathUtil.diffPercent(10009, 10000));

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);

        Assert.assertEquals("+" + nf.format(0.1) + "%", MathUtil.diffPercent(10011, 10000));
        Assert.assertEquals("-" + nf.format(0.1) + "%", MathUtil.diffPercent( 9989, 10000));

        Assert.assertEquals("+" + nf.format(5) + "%", MathUtil.diffPercent(1050, 1000));
        Assert.assertEquals("-" + nf.format(5) + "%", MathUtil.diffPercent( 950, 1000));
    }

}
