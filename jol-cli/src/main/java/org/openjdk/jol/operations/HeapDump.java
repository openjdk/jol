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
import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.datamodel.X86_32_DataModel;
import org.openjdk.jol.datamodel.X86_64_COOPS_DataModel;
import org.openjdk.jol.datamodel.X86_64_DataModel;
import org.openjdk.jol.heap.HeapDumpReader;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.layouters.RawLayouter;
import org.openjdk.jol.util.Multiset;

import java.io.File;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class HeapDump implements Operation {

    @Override
    public String label() {
        return "heapdump";
    }

    @Override
    public String description() {
        return "Consume the heap dump and estimate the savings in different layout strategies.";
    }

    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.err.println("Expected a hprof file name.");
            return;
        }
        String path = args[0];

        out.println("Heap Dump: " + path);
        out.println("Estimated heap consumed, bytes:");

        HeapDumpReader reader = new HeapDumpReader(new File(path));
        Multiset<ClassData> data = reader.parse();

        for (DataModel model : new DataModel[]{new X86_32_DataModel(), new X86_64_DataModel(), new X86_64_COOPS_DataModel(), new X86_64_COOPS_DataModel(16)}) {

            Layouter l = new RawLayouter(model);
            long rawData = process(data, l);
            out.printf("%11s %,15d: %s%n", "", rawData, l);

            l = new HotSpotLayouter(model);
            long hsBase = process(data, l);
            out.printf("%11s %,15d: %s%n", "", hsBase, l);

            final boolean[] BOOLS = {false, true};

            for (boolean hierarchyGaps : BOOLS) {
                for (boolean superClassGaps : BOOLS) {
                    for (boolean autoAlign : BOOLS) {
                        for (boolean compactFields : BOOLS) {
                            for (int fieldAllocationStyle : new int[]{0, 1, 2}) {
                                l = new HotSpotLayouter(model, hierarchyGaps, superClassGaps, autoAlign, compactFields, fieldAllocationStyle);
                                long s = process(data, l);
                                out.printf("%10.3f%% %,15d: %s%n", (s - hsBase) * 100.0 / hsBase, s, l);
                            }
                        }
                    }
                }
            }
            out.println();
        }
    }

    static long process(Multiset<ClassData> data, Layouter layouter) {
        long totalFootprint = 0;
        for (ClassData cd : data.keys()) {
            ClassLayout layout = layouter.layout(cd);
            totalFootprint += layout.instanceSize() * data.count(cd);
        }
        return totalFootprint;
    }

}
