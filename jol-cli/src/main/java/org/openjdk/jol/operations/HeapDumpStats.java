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
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.CurrentLayouter;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.layouters.RawLayouter;
import org.openjdk.jol.util.Multiset;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.System.in;
import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class HeapDumpStats implements Operation {

    @Override
    public String label() {
        return "heapdumpstats";
    }

    @Override
    public String description() {
        return "Consume the heap dump and print the most frequent instances.";
    }

    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.err.println("Expected a hprof file name.");
            return;
        }
        String path = args[0];

        out.println("Heap Dump: " + path);

        HeapDumpReader reader = new HeapDumpReader(new File(path));
        Multiset<ClassData> data = reader.parse();

        final Multiset<String> counts = new Multiset<String>();
        final Multiset<String> sizes = new Multiset<String>();

        Layouter layouter = new HotSpotLayouter(new CurrentDataModel());
        for (ClassData cd : data.keys()) {
            long size = layouter.layout(cd).instanceSize();
            counts.add(cd.name(), data.count(cd));
            sizes.add(cd.name(),  data.count(cd) * size);
        }

        List<String> sorted = new ArrayList<String>();
        sorted.addAll(sizes.keys());
        Collections.sort(sorted, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Long.valueOf(sizes.count(o2)).compareTo(sizes.count(o1));
            }
        });

        final int printFirst = Integer.getInteger("printFirst", 30);

        int idx = 0;
        out.printf(" %10s %10s %10s   %s%n", "COUNT", "AVG", "SIZE", "DESCRIPTION");
        out.println("-------------------------------------------------------------------------");
        for (String name : sorted) {
            if (++idx > printFirst) break;
            long cnt = counts.count(name);
            long size = sizes.count(name);
            out.printf(" %10d %10d %10d   %s%n", cnt, size / cnt, size, name);
        }
        out.println("-------------------------------------------------------------------------");
        out.printf(" %10d %10s %10d   %s%n", counts.size(), "", sizes.size(), "(total)");
    }

}
