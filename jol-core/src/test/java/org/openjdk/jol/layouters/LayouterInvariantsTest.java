package org.openjdk.jol.layouters;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jol.datamodel.CurrentDataModel;
import org.openjdk.jol.datamodel.*;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.util.ClassGenerator;

import java.util.Random;

public class LayouterInvariantsTest {

    private static final boolean[] BOOLS = {false, true};
    private static final DataModel[] MODELS = {
        new CurrentDataModel(),
        new X86_32_DataModel(),
        new X86_64_COOPS_DataModel(),
        new X86_64_DataModel()
    };
    private static final int ITERATIONS = 20000;

    @Test
    public void testRaw() throws Exception {
        Random seeder = new Random();
        for (int c = 0; c < ITERATIONS; c++) {
            int seed = seeder.nextInt();
            Class<?> cl = ClassGenerator.generate(new Random(seed), 5, 50);

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
            Class<?> cl = ClassGenerator.generate(new Random(seed), 5, 50);

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
            Class<?> cl = ClassGenerator.generate(new Random(seed), 5, 50);
            ClassData cd = ClassData.parseClass(cl);
            try {
                for (DataModel model : MODELS) {
                    for (boolean hierarchyGaps : BOOLS) {
                        for (boolean superClassGaps : BOOLS) {
                            for (boolean autoAlign : BOOLS) {
                                for (boolean compactFields : BOOLS) {
                                    for (int fieldAllocationStyle : new int[]{0, 1, 2}) {
                                        HotSpotLayouter layouter = new HotSpotLayouter(model,
                                                hierarchyGaps, superClassGaps, autoAlign,
                                                compactFields, fieldAllocationStyle);
                                        layouter.layout(cd);
                                    }
                                }
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
