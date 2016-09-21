package org.openjdk.jol.layouters;

import junit.framework.Assert;
import org.junit.Test;
import org.openjdk.jol.datamodel.CurrentDataModel;
import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.util.ClassGenerator;

import java.util.*;

public class HotspotLayouterRealTest {

    private static final boolean[] BOOLS = {false, true};
    private static final DataModel[] MODELS = { new CurrentDataModel() };
    private static final int ITERATIONS = 20000;

    @Test
    public void testSingleClass() throws Exception {
        tryWith(1, 5);
    }

    @Test
    public void testTwoClasses() throws Exception {
        tryWith(2, 5);
    }

    public void tryWith(int hierarchyDepth, int fieldsPerClass) throws Exception {
        Random seeder = new Random();
        for (int c = 0; c < ITERATIONS; c++) {
            int seed = seeder.nextInt();
            Class<?> cl = ClassGenerator.generate(new Random(seed), hierarchyDepth, fieldsPerClass);

            try {
                ClassLayout actual = ClassLayout.parseClass(cl);

                Map<Layouter, ClassLayout> candidates = candidateLayouts(cl);

                if (!candidates.values().contains(actual)) {
                    System.out.println(actual.toPrintable());
                    for (Layouter l : candidates.keySet()) {
                        System.out.println(l);
                        System.out.println(candidates.get(l).toPrintable());
                    }
                    Assert.fail("Actual layout should have matched at least one model layout. Seed = " + seed);
                }
            } catch (Exception e) {
                Assert.fail("Failed. Seed = " + seed);
            }
        }
    }

    private Map<Layouter, ClassLayout> candidateLayouts(Class<?> cl) {
        ClassData cd = ClassData.parseClass(cl);
        Map<Layouter, ClassLayout> layouts = new HashMap<Layouter, ClassLayout>();
        for (DataModel model : MODELS) {
            for (boolean hierarchyGaps : BOOLS) {
                for (boolean superClassGaps : BOOLS) {
                    for (boolean autoAlign : BOOLS) {
                        for (boolean compactFields : BOOLS) {
                            for (int fieldAllocationStyle : new int[]{0, 1, 2}) {
                                HotSpotLayouter layouter = new HotSpotLayouter(model,
                                        hierarchyGaps, superClassGaps, autoAlign,
                                        compactFields, fieldAllocationStyle);
                                layouts.put(layouter, layouter.layout(cd));
                            }
                        }
                    }
                }
            }
        }
        return layouts;
    }

}
