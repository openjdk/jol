package org.openjdk.jol.ljv.provider.impl;

import org.junit.Test;
import org.junit.Assert;
import org.openjdk.jol.ljv.VersionGuardedTest;

import static org.junit.Assume.assumeTrue;
import static org.openjdk.jol.ljv.provider.impl.ChangingArrayElementHighlighter.HIGHLIGHT;

public class ChangingArrayElementHighlighterTest extends VersionGuardedTest {

    ChangingArrayElementHighlighter provider = new ChangingArrayElementHighlighter();

    @Test
    public void checksChangedElements() {
        assumeTrue(is11());
        int[] arr = new int[]{1, 2, 3};
        for (int i = 0; i < arr.length; i++) {

            Assert.assertEquals("", provider.getAttribute(arr, i));
        }
        arr[0] = 2;
        arr[2] = 4;

        Assert.assertEquals(HIGHLIGHT, provider.getAttribute(arr, 0));
        Assert.assertEquals("", provider.getAttribute(arr, 1));
        Assert.assertEquals(HIGHLIGHT, provider.getAttribute(arr, 2));
    }
}