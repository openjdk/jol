package org.openjdk.jol.ljv.provider.impl;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.ljv.VersionGuardedTest;

import static org.junit.Assume.assumeTrue;
import static org.openjdk.jol.ljv.provider.impl.NewObjectHighlighter.HIGHLIGHT;

public class NewObjectHighlighterTest extends VersionGuardedTest {
    @Test
    public void newObjectsAreHighlighted() {
        assumeTrue(is11());
        Object o1 = new Object();
        Object o2 = new Object();
        NewObjectHighlighter highlighter = new NewObjectHighlighter();
        Assert.assertEquals(HIGHLIGHT, highlighter.getAttribute(o1));
        Assert.assertEquals(HIGHLIGHT, highlighter.getAttribute(o2));
        Assert.assertEquals("", highlighter.getAttribute(o1));
    }
}