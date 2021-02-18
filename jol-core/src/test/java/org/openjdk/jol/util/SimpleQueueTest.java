package org.openjdk.jol.util;

import org.junit.Assert;
import org.junit.Test;

public class SimpleQueueTest {

    @Test
    public void testSingle() {
        SimpleStack<Object> q = new SimpleStack<>();

        Object o1 = new Object();

        Assert.assertTrue(q.isEmpty());
        q.push(o1);
        Assert.assertFalse(q.isEmpty());
        Object r1 = q.pop();
        Assert.assertEquals(o1, r1);
        Assert.assertTrue(q.isEmpty());
    }

    @Test
    public void testOrder() {
        SimpleStack<Object> q = new SimpleStack<>();

        Object o1 = new Object();
        Object o2 = new Object();

        Assert.assertTrue(q.isEmpty());
        q.push(o1);
        q.push(o2);
        Assert.assertFalse(q.isEmpty());
        Object r2 = q.pop();
        Object r1 = q.pop();
        Assert.assertEquals(o1, r1);
        Assert.assertEquals(o2, r2);
        Assert.assertTrue(q.isEmpty());
    }

    @Test
    public void testGrow() {
        SimpleStack<Object> q = new SimpleStack<>();

        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();

        Assert.assertTrue(q.isEmpty());
        q.push(o1);
        q.push(o2);
        q.push(o3);
        Assert.assertFalse(q.isEmpty());
        Object r3 = q.pop();
        Object r2 = q.pop();
        Object r1 = q.pop();
        Assert.assertEquals(o1, r1);
        Assert.assertEquals(o2, r2);
        Assert.assertEquals(o3, r3);
        Assert.assertTrue(q.isEmpty());
    }

}
