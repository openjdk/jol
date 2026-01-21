package org.openjdk.jol.layouters;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.datamodel.ModelVM;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.util.ClassGenerator;

import java.util.Random;

public class HotspotLayouterCompactTest {

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

        HotSpotLayouter layouter8  = new HotSpotLayouter(new ModelVM(), 8);
        HotSpotLayouter layouter15 = new HotSpotLayouter(new ModelVM(), 15);
        HotSpotLayouter layouter23 = new HotSpotLayouter(new ModelVM(), 23);
        HotSpotLayouter layouter25 = new HotSpotLayouter(new ModelVM(), 25);

        ClassLayout layout8  = layouter8.layout(cd);
        ClassLayout layout15 = layouter15.layout(cd);
        ClassLayout layout23 = layouter23.layout(cd);
        ClassLayout layout25 = layouter25.layout(cd);

        Assert.assertTrue("JDK 15 is better than JDK 8: \n" + layout15 + "\n" + layout8,
                layout15.instanceSize() <= layout8.instanceSize());
        Assert.assertTrue("JDK 23 is better than JDK 15: \n" + layout23 + "\n" + layout15,
                layout23.instanceSize() <= layout15.instanceSize());
        Assert.assertTrue("JDK 25 is better than JDK 23: \n" + layout25 + "\n" + layout23,
                layout25.instanceSize() <= layout23.instanceSize());
    }

    public void tryWithClasses(int hierarchyDepth, int fieldsPerClass) throws Exception {
        Random seeder = new Random();

        for (int c = 0; c < ITERATIONS; c++) {
            int seed = seeder.nextInt();
            Class<?> cl = ClassGenerator.generate(new Random(seed), hierarchyDepth, fieldsPerClass);

            ClassLayout actual = ClassLayout.parseClass(cl);
            ClassData cd = ClassData.parseClass(cl);

            HotSpotLayouter layouter8  = new HotSpotLayouter(new ModelVM(), 8);
            HotSpotLayouter layouter15 = new HotSpotLayouter(new ModelVM(), 15);
            HotSpotLayouter layouter23 = new HotSpotLayouter(new ModelVM(), 23);
            HotSpotLayouter layouter25 = new HotSpotLayouter(new ModelVM(), 25);

            ClassLayout layout8  = layouter8.layout(cd);
            ClassLayout layout15 = layouter15.layout(cd);
            ClassLayout layout23 = layouter23.layout(cd);
            ClassLayout layout25 = layouter25.layout(cd);

            Assert.assertTrue("JDK 15 is better than JDK 8: \n" + layout15 + "\n" + layout8,
                    layout15.instanceSize() <= layout8.instanceSize());
            Assert.assertTrue("JDK 23 is better than JDK 15: \n" + layout23 + "\n" + layout15,
                    layout23.instanceSize() <= layout15.instanceSize());
            Assert.assertTrue("JDK 25 is better than JDK 23: \n" + layout25 + "\n" + layout23,
                    layout25.instanceSize() <= layout23.instanceSize());
        }
    }

}
