/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jol.operations;

import org.openjdk.jol.Operation;
import org.openjdk.jol.datamodel.ModelVM;
import org.openjdk.jol.heap.HeapDumpReader;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.util.ASCIITable;
import org.openjdk.jol.util.ClassUtils;
import org.openjdk.jol.util.Multiset;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class HeapDumpBoxes implements Operation {

    static final Class<?>[] PRIMITIVE_CLASSES = {
            Boolean.class,
            Byte.class,
            Short.class,
            Character.class,
            Integer.class,
            Float.class,
            Long.class,
            Double.class
    };

    static final Class<?>[] PRIMITIVE_CACHE_CLASSES = {
            Short.class,
            Character.class,
            Integer.class,
            Float.class,
            Long.class,
            Double.class
    };

    @Override
    public String label() {
        return "heapdump-boxes";
    }

    @Override
    public String description() {
        return "Read a heap dump and look for data that looks duplicated, focusing on primitive boxes";
    }

    private int getVMVersion() {
        try {
            return Integer.parseInt(System.getProperty("java.specification.version"));
        } catch (Exception e) {
            return 8;
        }
    }

    private long manualMarginalCost;
    private long arrayMarginalCost;

    public void computeMarginalCosts() {
        final int base = 1_234_567;
        final int size = 1_000_000;
        HashMap<Integer, Integer> empty = new HashMap<>();
        empty.put(base, base);
        empty.remove(base);

        HashMap<Integer, Integer> full = new HashMap<>();
        for (int i = base; i < base + size; i++) {
            full.put(i, i);
        }

        long intSize = ClassLayout.parseClass(Integer.class).instanceSize();
        long emptySize = GraphLayout.parseInstance(empty).totalSize();
        long fullSize = GraphLayout.parseInstance(full).totalSize();

        manualMarginalCost = (fullSize - emptySize) / size - intSize;

        long arraySize = ClassLayout.parseInstance(new Integer[size]).instanceSize();
        arrayMarginalCost = arraySize / size;
    }

    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.err.println("Expected a hprof file name.");
            return;
        }
        String path = args[0];

        Layouter layouter = new HotSpotLayouter(new ModelVM(), getVMVersion());

        computeMarginalCosts();

        out.println("Heap Dump: " + path);

        HeapDumpReader.MultiplexingVisitor mv = new HeapDumpReader.MultiplexingVisitor();

        Map<Class<?>, BoxVisitor> visitors = new HashMap<>();
        for (Class<?> cl : PRIMITIVE_CLASSES) {
            BoxVisitor v = new BoxVisitor(cl);
            visitors.put(cl, v);
            mv.add(v);
        }

        HeapDumpReader reader = new HeapDumpReader(new File(path), out, mv);
        Multiset<ClassData> data = reader.parse();

        out.println();
        out.println(layouter);
        out.println();

        long totalSize = 0;
        long totalCount = 0;
        for (ClassData cd : data.keys()) {
            totalSize += layouter.layout(cd).instanceSize() * data.count(cd);
            totalCount += data.count(cd);
        }

        out.printf("Heap dump contains %,d objects, %,d bytes in total.%n", totalCount, totalSize);
        out.println();

        for (Class<?> cl : PRIMITIVE_CLASSES) {
            BoxVisitor v = visitors.get(cl);
            v.printOut(out);
        }

        for (Class<?> cl : PRIMITIVE_CACHE_CLASSES) {
            BoxVisitor v = visitors.get(cl);
            v.printOut(out);
        }
    }

    public class BoxVisitor extends HeapDumpReader.Visitor {
        private final Multiset<Number> values = new Multiset<>();
        private final String clName;
        private final Class<?> cl;

        public BoxVisitor(Class<?> cl) {
            this.clName = ClassUtils.humanReadableName(cl);
            this.cl = cl;
        }

        @Override
        public void visitInstance(long id, long klassID, byte[] bytes, String name) {
            if (name.equals(clName)) {
                switch (clName) {
                    case "java.lang.Byte":
                    case "java.lang.Boolean":
                        values.add(ByteBuffer.wrap(bytes).get());
                        break;
                    case "java.lang.Character":
                    case "java.lang.Short":
                        values.add(ByteBuffer.wrap(bytes).getShort());
                        break;
                    case "java.lang.Integer":
                        values.add(ByteBuffer.wrap(bytes).getInt());
                        break;
                    case "java.lang.Float":
                        values.add(ByteBuffer.wrap(bytes).getFloat());
                        break;
                    case "java.lang.Long":
                        values.add(ByteBuffer.wrap(bytes).getLong());
                        break;
                    case "java.lang.Double":
                        values.add(ByteBuffer.wrap(bytes).getDouble());
                        break;
                    default:
                        throw new IllegalStateException("Unknown class: " + clName);
                }
            }
        }

        public void printOut(PrintStream ps) {
            boolean hasEntries = false;
            for (Number v : values.keys()) {
                long count = values.count(v) - 1;
                if (count > 0) {
                    hasEntries = true;
                    break;
                }
            }

            if (!hasEntries) {
                // Empty, nothing to print.
                return;
            }

            long instanceSize = ClassLayout.parseClass(cl).instanceSize();

            ASCIITable boxesTable = new ASCIITable(
                    false,
                    Integer.MAX_VALUE,
                    "=== " + clName + " boxes:",
                    "DUPS", "SUM BYTES", "VALUE");

            Multiset<Integer> autoBoxCountWins = new Multiset<>();
            Multiset<Integer> autoBoxSizeWins = new Multiset<>();

            List<Integer> limits = new ArrayList<>();
            for (long i = 256; i <= 1024 * 1024 * 1024; i *= 2) {
                limits.add((int) i);
            }

            for (Number v : values.keys()) {
                long count = values.count(v) - 1;

                if (count > 0) {
                    long size = count * instanceSize;
                    boxesTable.addLine((Comparable) v, count, size);

                    for (int limit : limits) {
                        if (-128 <= v.longValue() && v.longValue() < limit) {
                            autoBoxCountWins.add(limit, count);
                            autoBoxSizeWins.add(limit, size);
                        }
                    }
                }
            }

            boxesTable.print(ps, 2);

            ASCIITable arrayTable = new ASCIITable(
                    false,
                    Integer.MAX_VALUE,
                    (cl.equals(Integer.class) ?
                            "=== " + clName + ", savings with manual cache, or non-default AutoBoxCacheMax:" :
                            "=== " + clName + ", savings with manual cache:"),
                    "SAVED INSTANCES", "SAVED BYTES", "CACHE SHAPE");

            for (int limit : limits) {
                // Subtract the overhead for larger holding array
                long sizes = autoBoxSizeWins.count(limit) - arrayMarginalCost*(limit - 128);
                arrayTable.addLine(cl.getSimpleName() + "[" + limit + "]", autoBoxCountWins.count(limit), sizes);
            }

            arrayTable.print(ps, -1);

            Multiset<Integer> manualCachePopulation = new Multiset<>();
            Multiset<Integer> manualCacheCountWins = new Multiset<>();
            Multiset<Integer> manualCacheSizeWins = new Multiset<>();

            List<Number> sortedByCount = new ArrayList<>(values.keys());
            sortedByCount.sort((c1, c2) -> Long.compare(values.count(c2), values.count(c1)));

            int n = 0;
            for (Number v : sortedByCount) {
                long count = values.count(v) - 1;

                if (count > 0) {
                    long size = count * instanceSize;

                    for (int limit : limits) {
                        if (n < limit) {
                            manualCacheCountWins.add(limit, count);
                            manualCacheSizeWins.add(limit, size);
                            manualCachePopulation.add(limit);
                        }
                    }
                    n++;
                }
            }

            String mapName = "HashMap<" + cl.getSimpleName() + ", " + cl.getSimpleName() + ">";
            ASCIITable manualTable = new ASCIITable(
                    false,
                    Integer.MAX_VALUE,
                    "=== " + clName + ", savings with manual cache:",
                    "SAVED INSTANCES", "SAVED BYTES", "CACHE SHAPE");

            for (int limit : limits) {
                long sizes = manualCacheSizeWins.count(limit) - manualMarginalCost*manualCachePopulation.count(limit);
                manualTable.addLine(mapName + "(" + limit + ")", manualCacheSizeWins.count(limit), sizes);
            }

            manualTable.print(ps, -1);
        }

    }

}
