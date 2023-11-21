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
import org.openjdk.jol.util.ASCIITable;
import org.openjdk.jol.util.Multiset;

import java.io.File;

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

        out.println("Heap Dump: " + path);

        HeapDumpReader reader = new HeapDumpReader(new File(path), out, null);
        Multiset<ClassData> data = reader.parse();

        out.println();
        out.println(layouter);
        out.println();

        ASCIITable table = new ASCIITable(
                true,
                "=== Class Histogram",
                "INSTANCES", "SIZE", "SUM SIZE", "CLASS");

        for (ClassData cd : data.keys()) {
            long cnt = data.count(cd);
            if (cnt > 0) {
                long instanceSize = layouter.layout(cd).instanceSize();
                table.addLine(cd.prettyName(), cnt, instanceSize, cnt * instanceSize);
            }
        }

        table.print(out, 0);
        table.print(out, 1);
        table.print(out, 2);
    }

}
