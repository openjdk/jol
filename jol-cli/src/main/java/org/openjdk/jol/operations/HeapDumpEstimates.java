/*
 * Copyright (c) 2023, Red Hat, Inc. All rights reserved.
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
import org.openjdk.jol.layouters.RawLayouter;
import org.openjdk.jol.util.Multiset;

import java.io.File;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class HeapDumpEstimates implements Operation {

    @Override
    public String label() {
        return "heapdump-estimates";
    }

    @Override
    public String description() {
        return "Consume the heap dump and simulate the class layout in different VM modes";
    }

    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.err.println("Expected a hprof file name.");
            return;
        }
        String path = args[0];

        out.println("Heap Dump: " + path);
        out.println();

        HeapDumpReader reader = new HeapDumpReader(new File(path));
        Multiset<ClassData> data = reader.parse();

        long rawSize = 0;
        {
            RawLayouter rawLayouter = new RawLayouter(new Model32());
            for (ClassData cd : data.keys()) {
                rawSize += rawLayouter.layout(cd).instanceSize() * data.count(cd);
            }
        }

        for (DataModel model : EstimatedModels.MODELS_JDK8) {
            Layouter l = new HotSpotLayouter(model, 8);
            simulateWith(l, data, rawSize);
        }

        for (DataModel model : EstimatedModels.MODELS_JDK8) {
            Layouter l = new HotSpotLayouter(model, 15);
            simulateWith(l, data, rawSize);
        }

        for (DataModel model : EstimatedModels.MODELS_JDK15) {
            Layouter l = new HotSpotLayouter(model, 15);
            simulateWith(l, data, rawSize);
        }

        for (DataModel model : EstimatedModels.MODELS_LILLIPUT) {
            Layouter l = new HotSpotLayouter(model, 99);
            simulateWith(l, data, rawSize);
        }
    }

    private void simulateWith(Layouter layouter, Multiset<ClassData> data, long rawSize) {
        out.println("***** " + layouter);
        long size = 0L;
        for (ClassData cd : data.keys()) {
            size += layouter.layout(cd).instanceSize() * data.count(cd);
        }
        out.printf("  Total data size:   %d bytes%n", rawSize);
        out.printf("  Total object size: %d bytes%n", size);
        out.printf("  Object overhead:   %.1f%%%n", (size - rawSize) * 100.0 / size);
        out.println();
    }

}
