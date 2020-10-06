package org.openjdk.jol.layouters;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openjdk.jol.datamodel.CurrentDataModel;
import org.openjdk.jol.datamodel.*;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.util.ClassGenerator;

import java.util.Random;

public class LayouterInvariantsTest {

    private static final DataModel[] MODELS = {
        new CurrentDataModel(),
        new X86_32_DataModel(),
        new X86_64_COOPS_DataModel(),
        new X86_64_DataModel()
    };

    private static final int ITERATIONS = 10000;

    private static Class<?>[] CLS;
    private static int[] SEEDS;

    @BeforeClass
    public static void buildClasses() throws Exception {
        Random seeder = new Random();
        CLS = new Class<?>[ITERATIONS];
        SEEDS = new int[ITERATIONS];
        for (int c = 0; c < ITERATIONS; c++) {
            SEEDS[c] = seeder.nextInt();
            CLS[c] = ClassGenerator.generate(new Random(SEEDS[c]), 5, 50);
        }
    }

    @Test
    public void testRaw() {
        for (int c = 0; c < ITERATIONS; c++) {
            for (DataModel model : MODELS) {
                try {
                    ClassLayout.parseClass(CLS[c], new RawLayouter(model));
                } catch (Exception e) {
                    Assert.fail("Failed. Seed = " + SEEDS[c]);
                }
            }
        }
    }

    @Test
    public void testCurrent() {
        for (int c = 0; c < ITERATIONS; c++) {
            try {
                ClassLayout.parseClass(CLS[c], new CurrentLayouter());
            } catch (Exception e) {
                Assert.fail("Failed. Seed = " + SEEDS[c]);
            }
        }
    }

    @Test
    public void testHotspot() {
        for (int c = 0; c < ITERATIONS; c++) {
            ClassData cd = ClassData.parseClass(CLS[c]);
            try {
                for (DataModel model : MODELS) {
                    HotSpotLayouter layouter = new HotSpotLayouter(model);
                    layouter.layout(cd);
                }
            } catch (Exception e) {
                Assert.fail("Failed. Seed = " + SEEDS[c]);
            }
        }
    }

}
