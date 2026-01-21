package org.openjdk.jol.layouters;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jol.datamodel.ModelVM;
import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.util.ClassGenerator;

import java.util.*;

public class HotspotLayouterRealTest {

    private static final int ITERATIONS = 20000;

    private int getVersion() {
        try {
            return Integer.parseInt(System.getProperty("java.specification.version"));
        } catch (Exception e) {
            return -1;
        }
    }

    @Test
    public void testSingleClass() throws Exception {
        tryWithClasses(1, 5, getVersion());
    }

    @Test
    public void testTwoClasses() throws Exception {
        tryWithClasses(2, 5, getVersion());
    }

    @Test
    public void testThreeClasses() throws Exception {
        tryWithClasses(3, 6, getVersion());
    }

    @Test
    public void testArrays() {
        int version = getVersion();
        for (int c = 0; c < 128; c++) {
            tryWithArrays(new boolean[c], version);
            tryWithArrays(new byte[c],    version);
            tryWithArrays(new char[c],    version);
            tryWithArrays(new int[c],     version);
            tryWithArrays(new float[c],   version);
            tryWithArrays(new long[c],    version);
            tryWithArrays(new double[c],  version);
            tryWithArrays(new Object[c],  version);
        }
    }

    public void tryWithArrays(Object array, int jdkVersion) {
        ClassLayout actual = ClassLayout.parseInstance(array);
        ClassData cd = ClassData.parseInstance(array);

        HotSpotLayouter layouter = new HotSpotLayouter(new ModelVM(), jdkVersion);
        ClassLayout model = layouter.layout(cd);

        System.out.println(actual);
        System.out.println(model);

        Assert.assertEquals(model, actual);
    }

    public void tryWithClasses(int hierarchyDepth, int fieldsPerClass, int jdkVersion) throws Exception {
        Random seeder = new Random();
        HotSpotLayouter layouter = new HotSpotLayouter(new ModelVM(), jdkVersion);

        for (int c = 0; c < ITERATIONS; c++) {
            int seed = seeder.nextInt();
            Class<?> cl = ClassGenerator.generate(new Random(seed), hierarchyDepth, fieldsPerClass);

            ClassLayout actual = ClassLayout.parseClass(cl);
            ClassData cd = ClassData.parseClass(cl);

            ClassLayout model = layouter.layout(cd);
            Assert.assertEquals(model, actual);
        }
    }

}
