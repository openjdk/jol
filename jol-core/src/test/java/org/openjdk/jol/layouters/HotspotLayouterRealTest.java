package org.openjdk.jol.layouters;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jol.TestUtils;
import org.openjdk.jol.datamodel.ModelVM;
import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.util.ClassGenerator;

import java.util.*;

public class HotspotLayouterRealTest {

    private static final int ITERATIONS = 20000;

    @Test
    public void testSingleClass() throws Exception {
        tryWithClasses(1, 5);
    }

    @Test
    public void testTwoClasses() throws Exception {
        tryWithClasses(2, 5);
    }

    @Test
    public void testThreeClasses() throws Exception {
        tryWithClasses(3, 6);
    }

    @Test
    public void testArrays() {
        for (int c = 0; c < 128; c++) {
            tryWithArrays(new boolean[c]);
            tryWithArrays(new byte[c]);
            tryWithArrays(new char[c]);
            tryWithArrays(new int[c]);
            tryWithArrays(new float[c]);
            tryWithArrays(new long[c]);
            tryWithArrays(new double[c]);
            tryWithArrays(new Object[c]);
        }
    }

    public void tryWithArrays(Object array) {
        ClassLayout actual = ClassLayout.parseInstance(array);
        ClassData cd = ClassData.parseInstance(array);

        HotSpotLayouter layouter = new HotSpotLayouter(new ModelVM(), TestUtils.JDK_VERSION);
        ClassLayout model = layouter.layout(cd);

        System.out.println(actual);
        System.out.println(model);

        Assert.assertEquals(model, actual);
    }

    public void tryWithClasses(int hierarchyDepth, int fieldsPerClass) throws Exception {
        Random seeder = new Random();
        HotSpotLayouter layouter = new HotSpotLayouter(new ModelVM(), TestUtils.JDK_VERSION);

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
