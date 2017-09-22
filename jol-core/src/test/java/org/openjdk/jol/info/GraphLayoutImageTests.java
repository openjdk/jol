package org.openjdk.jol.info;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class GraphLayoutImageTests {

    @Test
    public void testNull() throws Exception {
        File file = File.createTempFile("jol", "imagetest");
        try {
            GraphLayout.parseInstance(null).toImage(file.getAbsolutePath());
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testNullVararg() throws Exception {
        Object obj = new Object();
        File file = File.createTempFile("jol", "imagetest");
        try {
            GraphLayout.parseInstance(obj, null).toImage(file.getAbsolutePath());
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testSingleObject() throws Exception {
        Object obj = new Object();

        File file = File.createTempFile("jol", "imagetest");
        GraphLayout.parseInstance(obj).toImage(file.getAbsolutePath());
        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.length() > 0);
    }

    @Test
    public void testSingleArray() throws Exception {
        int[] array = new int[10];

        File file = File.createTempFile("jol", "imagetest");
        GraphLayout.parseInstance(array).toImage(file.getAbsolutePath());
        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.length() > 0);
    }

    @Test
    public void testMultipleArrays() throws Exception {
        int[] arr1 = new int[10];
        int[] arr2 = new int[10];

        File file = File.createTempFile("jol", "imagetest");
        GraphLayout.parseInstance(arr1, arr2).toImage(file.getAbsolutePath());
        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.length() > 0);
    }

}
