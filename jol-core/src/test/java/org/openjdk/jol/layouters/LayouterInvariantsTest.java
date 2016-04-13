package org.openjdk.jol.layouters;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jol.datamodel.CurrentDataModel;
import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.info.ClassLayout;

import java.util.Random;

public class LayouterInvariantsTest {

    private static final boolean[] BOOLS = {false, true};
    private static final DataModel[] MODELS = {new CurrentDataModel()};
    private static final int ITERATIONS = 20000;

    @Test
    public void testRaw() throws Exception {
        Random seeder = new Random();
        for (int c = 0; c < ITERATIONS; c++) {
            int seed = seeder.nextInt();
            Class<?> cl = ClassGenerator.generate(new Random(seed));

            for (DataModel model : MODELS) {
                try {
                    ClassLayout.parseClass(cl, new RawLayouter(model));
                } catch (Exception e) {
                    Assert.fail("Failed. Seed = " + seed);
                }
            }
        }
    }

    @Test
    public void testCurrent() throws Exception {
        Random seeder = new Random();
        for (int c = 0; c < ITERATIONS; c++) {
            int seed = seeder.nextInt();
            Class<?> cl = ClassGenerator.generate(new Random(seed));

            try {
                ClassLayout.parseClass(cl, new CurrentLayouter());
            } catch (Exception e) {
                Assert.fail("Failed. Seed = " + seed);
            }
        }
    }

    @Test
    public void testHotspot() throws Exception {
        Random seeder = new Random();
        for (int c = 0; c < ITERATIONS; c++) {
            int seed = seeder.nextInt();
            Class<?> cl = ClassGenerator.generate(new Random(seed));

            try {
                for (DataModel model : MODELS) {
                    for (boolean hierarchyGaps : BOOLS) {
                        for (boolean superClassGaps : BOOLS) {
                            for (boolean autoAlign : BOOLS) {
                                ClassLayout.parseClass(cl, new HotSpotLayouter(model, hierarchyGaps, superClassGaps, autoAlign));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Assert.fail("Failed. Seed = " + seed);
            }
        }
    }

}
