package org.openjdk.jol.util;

import org.junit.Assert;
import org.junit.Test;

public class ArrayDimensionParserTest {

    @Test
    public void parseEmptyString() {
        try {
            ArrayDimensionParser.parse("");
            Assert.fail("Parser mustn't accept empty string");
        } catch (IllegalStateException e) {
        } catch (Exception e) {
            Assert.fail("Wrong exception was thrown: " + e);
        }
    }

    @Test
    public void parseEmptyDims() {
        int[] dims = ArrayDimensionParser.parse("[]");
        Assert.assertArrayEquals(new int[]{0}, dims);

        dims = ArrayDimensionParser.parse("[][]");
        Assert.assertArrayEquals(new int[]{0, 0}, dims);
    }

    @Test
    public void parseDigitDims() {
        int[] dims = ArrayDimensionParser.parse("[1]");
        Assert.assertArrayEquals(new int[]{1}, dims);

        dims = ArrayDimensionParser.parse("[1][2]");
        Assert.assertArrayEquals(new int[]{1, 2}, dims);
    }

    @Test
    public void parseMixedDims() {
        int[] dims = ArrayDimensionParser.parse("[1][]");
        Assert.assertArrayEquals(new int[]{1, 0}, dims);

        dims = ArrayDimensionParser.parse("[][2]");
        Assert.assertArrayEquals(new int[]{0, 2}, dims);
    }
}
