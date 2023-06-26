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
        long rawCount = 0;
        {
            RawLayouter rawLayouter = new RawLayouter(new Model32());
            for (ClassData cd : data.keys()) {
                rawSize += rawLayouter.layout(cd).instanceSize() * data.count(cd);
                rawCount += data.count(cd);
            }
        }

        out.println("=== Overall Statistics");
        out.println();
        out.printf("%10s,     %s%n", inProperUnits(rawCount), "Total objects");
        out.printf("%10s,     %s%n", inProperUnits(rawSize), "Total data size");
        out.printf("%10s,     %s%n", String.format("%.2f", 1F * rawSize / rawCount), "Average data per object");
        out.println();

        final String msg_noCoops =          "64-bit, no comp refs (>32 GB heap, default align)";
        final String msg_noCoops_ccp =      "64-bit, no comp refs, but comp klasses (>32 GB heap, default align)";
        final String msg_coops =            "64-bit, comp refs (<32 GB heap, default align)";
        final String msg_coops_align16 =    "64-bit, comp refs with large align (32..64GB heap, 16-byte align)";
        final String msg_coops_align32 =    "64-bit, comp refs with large align (64..128GB heap, 32-byte align)";
        final String msg_coops_align64 =    "64-bit, comp refs with large align (128..256GB heap, 64-byte align)";
        final String msg_coops_align128 =   "64-bit, comp refs with large align (256..512GB heap, 128-byte align)";
        final String msg_coops_align256 =   "64-bit, comp refs with large align (512..1024GB heap, 256-byte align)";

        final String desc = "  'Overhead' comes from additional metadata, representation and alignment losses.\n" +
                "  'JVM mode' is relative footprint change compared to the best JVM mode in this JDK.\n" +
                "  'Upgrade From' is the relative footprint change against the same mode in other JDKs.\n";


        out.println("=== Stock 32-bit OpenJDK");
        out.println();
        out.println(desc);

        long jdk8_32 = computeWithLayouter(data, new HotSpotLayouter(new Model32(), 8));;
        {
            out.printf("%10s, %10s,     %s%n",
                    "Footprint", "Overhead", "Description"
            );

            out.printf("%10s, %10s,     %s%n",
                    inProperUnits(jdk8_32),
                    diff(jdk8_32, rawSize),
                    "32-bit (<4 GB heap)"
            );
        }
        out.println();

        out.println("=== Stock 64-bit OpenJDK (JDK < 15)");
        out.println();
        out.println(desc);

        long jdk8_coops =           computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true), 8));
        long jdk8_noCoops =         computeWithLayouter(data, new HotSpotLayouter(new Model64(false, false), 8));
        long jdk8_coops_align16 =   computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 16), 8));
        long jdk8_coops_align32 =   computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 32), 8));
        long jdk8_coops_align64 =   computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 64), 8));
        long jdk8_coops_align128 =  computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 128), 8));
        long jdk8_coops_align256 =  computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 256), 8));

        {
            out.printf("%10s, %10s, %10s,     %s%n",
                    "Footprint", "Overhead", "JVM Mode", "Description"
            );

            out.printf("%10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk8_noCoops),
                    diff(jdk8_noCoops, rawSize),
                    diff(jdk8_noCoops, jdk8_coops),
                    msg_noCoops
            );

            out.printf("%10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk8_coops),
                    diff(jdk8_coops, rawSize),
                    diff(jdk8_coops, jdk8_coops),
                    msg_coops
            );

            out.printf("%10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk8_coops_align16),
                    diff(jdk8_coops_align16, rawSize),
                    diff(jdk8_coops_align16, jdk8_coops),
                    msg_coops_align16
            );

            out.printf("%10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk8_coops_align32),
                    diff(jdk8_coops_align32, rawSize),
                    diff(jdk8_coops_align32, jdk8_coops),
                    msg_coops_align32
            );

            out.printf("%10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk8_coops_align64),
                    diff(jdk8_coops_align64, rawSize),
                    diff(jdk8_coops_align64, jdk8_coops),
                    msg_coops_align64
            );

            out.printf("%10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk8_coops_align128),
                    diff(jdk8_coops_align128, rawSize),
                    diff(jdk8_coops_align128, jdk8_coops),
                    msg_coops_align128
            );

            out.printf("%10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk8_coops_align256),
                    diff(jdk8_coops_align256, rawSize),
                    diff(jdk8_coops_align256, jdk8_coops),
                    msg_coops_align256
            );
        }
        out.println();

        long jdk15_coops =          computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true), 15));
        long jdk15_noCoops =        computeWithLayouter(data, new HotSpotLayouter(new Model64(false, true), 15));
        long jdk15_coops_align16 =  computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 16), 15));
        long jdk15_coops_align32 =  computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 32), 15));
        long jdk15_coops_align64 =  computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 64), 15));
        long jdk15_coops_align128 = computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 128), 15));
        long jdk15_coops_align256 = computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 256), 15));

        out.println("=== Stock 64-bit OpenJDK (JDK >= 15)");
        out.println();
        out.println(desc);

        {
            out.printf("%37s %s%n", "", "Upgrade From:");
            out.printf("%10s, %10s, %10s, %10s,     %s%n",
                    "Footprint", "Overhead", "JVM Mode", "JDK < 15", "Description"
            );

            out.printf("%10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk15_noCoops),
                    diff(jdk15_noCoops, rawSize),
                    diff(jdk15_noCoops, jdk15_coops),
                    diff(jdk15_noCoops, jdk8_noCoops),
                    msg_noCoops_ccp
            );

            out.printf("%10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk15_coops),
                    diff(jdk15_coops, rawSize),
                    diff(jdk15_coops, jdk15_coops),
                    diff(jdk15_coops, jdk8_coops),
                    msg_coops
            );

            out.printf("%10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk15_coops_align16),
                    diff(jdk15_coops_align16, rawSize),
                    diff(jdk15_coops_align16, jdk15_coops),
                    diff(jdk15_coops_align16, jdk8_coops_align16),
                    msg_coops_align16
            );

            out.printf("%10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk15_coops_align32),
                    diff(jdk15_coops_align32, rawSize),
                    diff(jdk15_coops_align32, jdk15_coops),
                    diff(jdk15_coops_align32, jdk8_coops_align32),
                    msg_coops_align32
            );

            out.printf("%10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk15_coops_align64),
                    diff(jdk15_coops_align64, rawSize),
                    diff(jdk15_coops_align64, jdk15_coops),
                    diff(jdk15_coops_align64, jdk8_coops_align64),
                    msg_coops_align64
            );

            out.printf("%10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk15_coops_align128),
                    diff(jdk15_coops_align128, rawSize),
                    diff(jdk15_coops_align128, jdk15_coops),
                    diff(jdk15_coops_align128, jdk8_coops_align128),
                    msg_coops_align128
            );

            out.printf("%10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdk15_coops_align256),
                    diff(jdk15_coops_align256, rawSize),
                    diff(jdk15_coops_align256, jdk15_coops),
                    diff(jdk15_coops_align256, jdk8_coops_align256),
                    msg_coops_align256
            );
        }
        out.println();

        out.println("=== Experimental 64-bit OpenJDK: Lilliput, 64-bit headers");
        out.println();
        out.println(desc);

        long jdkLilliput_coops =            computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 8, false), 99));
        long jdkLilliput_noCoops =          computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(false, 8, false), 99));
        long jdkLilliput_coops_align16 =    computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 16, false), 99));
        long jdkLilliput_coops_align32 =    computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 32, false), 99));
        long jdkLilliput_coops_align64 =    computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 64, false), 99));
        long jdkLilliput_coops_align128 =   computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 128, false), 99));
        long jdkLilliput_coops_align256 =   computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 256, false), 99));

        {
            out.printf("%37s %s%n", "", "Upgrade From:");
            out.printf("%10s, %10s, %10s, %10s, %10s,     %s%n",
                    "Footprint", "Overhead", "JVM Mode", "JDK < 15", "JDK >= 15", "Description"
            );

            out.printf("%10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput_noCoops),
                    diff(jdkLilliput_noCoops, rawSize),
                    diff(jdkLilliput_noCoops, jdkLilliput_coops),
                    diff(jdkLilliput_noCoops, jdk8_noCoops),
                    diff(jdkLilliput_noCoops, jdk15_noCoops),
                    msg_noCoops_ccp
            );

            out.printf("%10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput_coops),
                    diff(jdkLilliput_coops, rawSize),
                    diff(jdkLilliput_coops, jdkLilliput_coops),
                    diff(jdkLilliput_coops, jdk8_coops),
                    diff(jdkLilliput_coops, jdk15_coops),
                    msg_coops
            );

            out.printf("%10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput_coops_align16),
                    diff(jdkLilliput_coops_align16, rawSize),
                    diff(jdkLilliput_coops_align16, jdkLilliput_coops),
                    diff(jdkLilliput_coops_align16, jdk8_coops_align16),
                    diff(jdkLilliput_coops_align16, jdk15_coops_align16),
                    msg_coops_align16
            );

            out.printf("%10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput_coops_align32),
                    diff(jdkLilliput_coops_align32, rawSize),
                    diff(jdkLilliput_coops_align32, jdkLilliput_coops),
                    diff(jdkLilliput_coops_align32, jdk8_coops_align32),
                    diff(jdkLilliput_coops_align32, jdk15_coops_align32),
                    msg_coops_align32
            );

            out.printf("%10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput_coops_align64),
                    diff(jdkLilliput_coops_align64, rawSize),
                    diff(jdkLilliput_coops_align64, jdkLilliput_coops),
                    diff(jdkLilliput_coops_align64, jdk8_coops_align64),
                    diff(jdkLilliput_coops_align64, jdk15_coops_align64),
                    msg_coops_align64
            );

            out.printf("%10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput_coops_align128),
                    diff(jdkLilliput_coops_align128, rawSize),
                    diff(jdkLilliput_coops_align128, jdkLilliput_coops),
                    diff(jdkLilliput_coops_align128, jdk8_coops_align128),
                    diff(jdkLilliput_coops_align128, jdk15_coops_align128),
                    msg_coops_align128
            );

            out.printf("%10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput_coops_align256),
                    diff(jdkLilliput_coops_align256, rawSize),
                    diff(jdkLilliput_coops_align256, jdkLilliput_coops),
                    diff(jdkLilliput_coops_align256, jdk8_coops_align256),
                    diff(jdkLilliput_coops_align256, jdk15_coops_align256),
                    msg_coops_align256
            );
        }
        out.println();

        out.println("=== Experimental 64-bit OpenJDK: Lilliput, 32-bit headers");
        out.println();
        out.println(desc);

        long jdkLilliput32_coops =          computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 8, true), 99));
        long jdkLilliput32_noCoops =        computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(false, 8, true), 99));
        long jdkLilliput32_coops_align16 =  computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 16, true), 99));
        long jdkLilliput32_coops_align32 =  computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 32, true), 99));
        long jdkLilliput32_coops_align64 =  computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 64, true), 99));
        long jdkLilliput32_coops_align128 = computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 128, true), 99));
        long jdkLilliput32_coops_align256 = computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 256, true), 99));

        {
            out.printf("%37s %s%n", "", "Upgrade From:");
            out.printf("%10s, %10s, %10s, %10s, %10s, %10s,     %s%n",
                    "Footprint", "Overhead", "JVM Mode", "JDK < 15", "JDK >= 15", "Lill-64", "Description"
            );

            out.printf("%10s, %10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput32_noCoops),
                    diff(jdkLilliput32_noCoops, rawSize),
                    diff(jdkLilliput32_noCoops, jdkLilliput32_coops),
                    diff(jdkLilliput32_noCoops, jdk8_noCoops),
                    diff(jdkLilliput32_noCoops, jdk15_noCoops),
                    diff(jdkLilliput32_noCoops, jdkLilliput_noCoops),
                    msg_noCoops_ccp
            );

            out.printf("%10s, %10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput32_coops),
                    diff(jdkLilliput32_coops, rawSize),
                    diff(jdkLilliput32_coops, jdkLilliput32_coops),
                    diff(jdkLilliput32_coops, jdk8_coops),
                    diff(jdkLilliput32_coops, jdk15_coops),
                    diff(jdkLilliput32_coops, jdkLilliput_coops),
                    msg_coops
            );

            out.printf("%10s, %10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput32_coops_align16),
                    diff(jdkLilliput32_coops_align16, rawSize),
                    diff(jdkLilliput32_coops_align16, jdkLilliput32_coops),
                    diff(jdkLilliput32_coops_align16, jdk8_coops_align16),
                    diff(jdkLilliput32_coops_align16, jdk15_coops_align16),
                    diff(jdkLilliput32_coops_align16, jdkLilliput_coops_align16),
                    msg_coops_align16
            );

            out.printf("%10s, %10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput32_coops_align32),
                    diff(jdkLilliput32_coops_align32, rawSize),
                    diff(jdkLilliput32_coops_align32, jdkLilliput32_coops),
                    diff(jdkLilliput32_coops_align32, jdk8_coops_align32),
                    diff(jdkLilliput32_coops_align32, jdk15_coops_align32),
                    diff(jdkLilliput32_coops_align32, jdkLilliput_coops_align32),
                    msg_coops_align32
            );

            out.printf("%10s, %10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput32_coops_align64),
                    diff(jdkLilliput32_coops_align64, rawSize),
                    diff(jdkLilliput32_coops_align64, jdkLilliput32_coops),
                    diff(jdkLilliput32_coops_align64, jdk8_coops_align64),
                    diff(jdkLilliput32_coops_align64, jdk15_coops_align64),
                    diff(jdkLilliput32_coops_align64, jdkLilliput_coops_align64),
                    msg_coops_align64
            );

            out.printf("%10s, %10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput32_coops_align128),
                    diff(jdkLilliput32_coops_align128, rawSize),
                    diff(jdkLilliput32_coops_align128, jdkLilliput32_coops),
                    diff(jdkLilliput32_coops_align128, jdk8_coops_align128),
                    diff(jdkLilliput32_coops_align128, jdk15_coops_align128),
                    diff(jdkLilliput32_coops_align128, jdkLilliput_coops_align128),
                    msg_coops_align128
            );

            out.printf("%10s, %10s, %10s, %10s, %10s, %10s,     %s%n",
                    inProperUnits(jdkLilliput32_coops_align256),
                    diff(jdkLilliput32_coops_align256, rawSize),
                    diff(jdkLilliput32_coops_align256, jdkLilliput32_coops),
                    diff(jdkLilliput32_coops_align256, jdk8_coops_align256),
                    diff(jdkLilliput32_coops_align256, jdk15_coops_align256),
                    diff(jdkLilliput32_coops_align256, jdkLilliput_coops_align256),
                    msg_coops_align256
            );
        }
        out.println();

    }

    private static long computeWithLayouter(Multiset<ClassData> data, Layouter layouter) {
        long size = 0L;
        for (ClassData cd : data.keys()) {
            size += layouter.layout(cd).instanceSize() * data.count(cd);
        }
        return size;
    }

    private String inProperUnits(long bytes) {
        final long K = 1000L;
        final long M = K * K;
        final long G = K * K * K;
        if (bytes > 100 * G) {
            return (bytes / G) + "G";
        } else if (bytes > 100 * M) {
            return (bytes / M) + "M";
        } else if (bytes > 100 * K) {
            return (bytes / K) + "K";
        } else {
            return bytes + "";
        }
    }

    private String diff(long size, long baseline) {
        if (size == baseline) {
            return "(same)";
        } else {
            return String.format("%+.1f%%", 100F * size / baseline - 100F);
        }
    }

}
