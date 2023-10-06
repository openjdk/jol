/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jol.datamodel.*;
import org.openjdk.jol.heap.HeapDumpReader;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.util.Multiset;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class HeapDumpStats implements Operation {

    @Override
    public String label() {
        return "heapdump-stats";
    }

    @Override
    public String description() {
        return "Read a heap dump and print simple statistics";
    }

    private int getVMVersion() {
        try {
            return Integer.parseInt(System.getProperty("java.specification.version"));
        } catch (Exception e) {
            return 8;
        }
    }

    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.err.println("Expected a hprof file name.");
            return;
        }
        String path = args[0];

        Layouter layouter = new HotSpotLayouter(new ModelVM(), getVMVersion());

        final int printFirst = Integer.getInteger("printFirst", 30);
        final String sortBy = System.getProperty("sort", "sum-size");

        final Multiset<String> counts = new Multiset<>();
        final Multiset<String> sizes = new Multiset<>();

        Comparator<String> sorter;
        switch (sortBy) {
            case "sum-size":
                sorter = (o1, o2) -> Long.compare(sizes.count(o2), sizes.count(o1));
                break;
            case "avg-size":
                sorter = (o1, o2) -> Double.compare(1D * sizes.count(o2) / counts.count(o2),
                                                    1D * sizes.count(o1) / counts.count(o1));
                break;
            case "instances":
                sorter = (o1, o2) -> Long.compare(counts.count(o2), counts.count(o1));
                break;
            default:
                throw new IllegalArgumentException("Cannot parse: " + sortBy);
        }

        out.println("Heap Dump: " + path);

        HeapDumpReader reader = new HeapDumpReader(new File(path), out, null);
        Multiset<ClassData> data = reader.parse();

        for (ClassData cd : data.keys()) {
            long size = layouter.layout(cd).instanceSize();
            counts.add(cd.name(), data.count(cd));
            sizes.add(cd.name(),  data.count(cd) * size);
        }

        List<String> sorted = new ArrayList<>(sizes.keys());
        sorted.sort(sorter);

        out.println();
        out.println(layouter);
        out.println();
        out.println("Sorting by " + sortBy + ". Use -Dsort={sum-size,avg-size,instances} to override.");
        out.println();
        out.println("Printing first " + printFirst + " lines. Use -DprintFirst=# to override.");
        out.println();

        int idx = 0;
        long printedCnt = 0;
        long printedSize = 0;
        out.printf(" %13s %13s %13s   %s%n", "INSTANCES", "SUM SIZE", "AVG SIZE", "CLASS");
        out.println("------------------------------------------------------------------------------------------------");
        for (String name : sorted) {
            if (++idx > printFirst) break;
            long cnt = counts.count(name);
            long size = sizes.count(name);
            out.printf(" %13d %13d %13d   %s%n", cnt, size, size / cnt, name);
            printedCnt += cnt;
            printedSize += size;
        }
        if (sorted.size() > printFirst) {
            out.printf(" %13d %13d %13s   %s%n", counts.size() - printedCnt, sizes.size() - printedSize, "", "(other)");
        }
        out.println("------------------------------------------------------------------------------------------------");
        out.printf(" %13d %13d %13s   %s%n", counts.size(), sizes.size(), "", "(total)");
    }

}
