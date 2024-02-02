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
import org.openjdk.jol.util.MathUtil;
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
        return "Read a heap dump and estimate footprint in different VM modes";
    }

    public void run(String... args) throws Exception {
        if (args.length == 0) {
            System.err.println("Expected a hprof file name.");
            return;
        }
        String path = args[0];

        out.println("Heap Dump: " + path);
        out.println();

        out.println("'Overhead' comes from additional metadata, representation and alignment losses.");
        out.println("'JVM mode' is the relative footprint change compared to the best JVM mode in this JDK.");
        out.println("'Upgrade From' is the relative footprint change against the same mode in other JDKs.");
        out.println();

        HeapDumpReader reader = new HeapDumpReader(new File(path), out, null);
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
        out.printf("%10s,     %s%n", MathUtil.inProperUnits(rawCount), "Total objects");
        out.printf("%10s,     %s%n", MathUtil.inProperUnits(rawSize), "Total data size");
        out.printf("%10s,     %s%n", String.format("%.2f", 1F * rawSize / rawCount), "Average data per object");
        out.println();

        final String msg_noCoops =          "64-bit, no comp refs (>32 GB heap, default align)";
        final String msg_noCoops_ccp =      "64-bit, no comp refs, but comp classes (>32 GB heap, default align)";
        final String msg_coops =            "64-bit, comp refs (<32 GB heap, default align)";
        final String msg_coops_align16 =    "64-bit, comp refs with large align (   32..64GB heap,  16-byte align)";
        final String msg_coops_align32 =    "64-bit, comp refs with large align (  64..128GB heap,  32-byte align)";
        final String msg_coops_align64 =    "64-bit, comp refs with large align ( 128..256GB heap,  64-byte align)";
        final String msg_coops_align128 =   "64-bit, comp refs with large align ( 256..512GB heap, 128-byte align)";
        final String msg_coops_align256 =   "64-bit, comp refs with large align (512..1024GB heap, 256-byte align)";

        out.println("=== Stock 32-bit OpenJDK");
        out.println();

        long jdk8_32 = computeWithLayouter(data, new HotSpotLayouter(new Model32(), 8));
        {
            out.printf("%10s, %10s,     %s%n",
                    "Footprint", "Overhead", "Description"
            );

            printLine("32-bit (<4 GB heap)",              rawSize,    jdk8_32);
        }
        out.println();

        out.println("=== Stock 64-bit OpenJDK (JDK < 15)");
        out.println();

        long jdk8_noCoops =         computeWithLayouter(data, new HotSpotLayouter(new Model64(false, false), 8));
        long jdk8_coops =           computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true,   8), 8));
        long jdk8_coops_align16 =   computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true,  16), 8));
        long jdk8_coops_align32 =   computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true,  32), 8));
        long jdk8_coops_align64 =   computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true,  64), 8));
        long jdk8_coops_align128 =  computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 128), 8));
        long jdk8_coops_align256 =  computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 256), 8));

        {
            out.printf("%10s, %10s, %10s,     %s%n",
                    "Footprint", "Overhead", "JVM Mode", "Description"
            );

            printLine(msg_noCoops,              rawSize,    jdk8_noCoops,           jdk8_coops);
            printLine(msg_coops,                rawSize,    jdk8_coops,             jdk8_coops);
            printLine(msg_coops_align16,        rawSize,    jdk8_coops_align16,     jdk8_coops);
            printLine(msg_coops_align32,        rawSize,    jdk8_coops_align32,     jdk8_coops);
            printLine(msg_coops_align64,        rawSize,    jdk8_coops_align64,     jdk8_coops);
            printLine(msg_coops_align128,       rawSize,    jdk8_coops_align128,    jdk8_coops);
            printLine(msg_coops_align256,       rawSize,    jdk8_coops_align256,    jdk8_coops);
        }
        out.println();

        long jdk15_noCoops =        computeWithLayouter(data, new HotSpotLayouter(new Model64(false, true), 15));
        long jdk15_coops =          computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true,   8), 15));
        long jdk15_coops_align16 =  computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true,  16), 15));
        long jdk15_coops_align32 =  computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true,  32), 15));
        long jdk15_coops_align64 =  computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true,  64), 15));
        long jdk15_coops_align128 = computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 128), 15));
        long jdk15_coops_align256 = computeWithLayouter(data, new HotSpotLayouter(new Model64(true, true, 256), 15));

        out.println("=== Stock 64-bit OpenJDK (JDK >= 15)");
        out.println();

        {
            out.printf("%37s %s%n", "", "Upgrade From:");
            out.printf("%10s, %10s, %10s, %10s,     %s%n",
                    "Footprint", "Overhead", "JVM Mode", "JDK < 15", "Description"
            );

            printLine(msg_noCoops_ccp,      rawSize,    jdk15_noCoops,          jdk15_coops,    jdk8_noCoops);
            printLine(msg_coops,            rawSize,    jdk15_coops,            jdk15_coops,    jdk8_coops);
            printLine(msg_coops_align16,    rawSize,    jdk15_coops_align16,    jdk15_coops,    jdk8_coops_align16);
            printLine(msg_coops_align32,    rawSize,    jdk15_coops_align32,    jdk15_coops,    jdk8_coops_align32);
            printLine(msg_coops_align64,    rawSize,    jdk15_coops_align64,    jdk15_coops,    jdk8_coops_align64);
            printLine(msg_coops_align128,   rawSize,    jdk15_coops_align128,   jdk15_coops,    jdk8_coops_align128);
            printLine(msg_coops_align256,   rawSize,    jdk15_coops_align256,   jdk15_coops,    jdk8_coops_align256);
        }
        out.println();

        out.println("=== Experimental 64-bit OpenJDK: Lilliput, 64-bit headers");
        out.println();

        long jdkLilliput_noBase_noCoops =          computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(false,  8,    8, false), 99));
        long jdkLilliput_noBase_coops =            computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,   8,    8, false), 99));
        long jdkLilliput_noBase_coops_align16 =    computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,  16,    8, false), 99));
        long jdkLilliput_noBase_coops_align32 =    computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,  32,    8, false), 99));
        long jdkLilliput_noBase_coops_align64 =    computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,  64,    8, false), 99));
        long jdkLilliput_noBase_coops_align128 =   computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 128,    8, false), 99));
        long jdkLilliput_noBase_coops_align256 =   computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 256,    8, false), 99));

        {
            out.printf("%37s %s%n", "", "Upgrade From:");
            out.printf("%10s, %10s, %10s, %10s, %10s,     %s%n",
                    "Footprint", "Overhead", "JVM Mode", "JDK < 15", "JDK >= 15", "Description"
            );

            printLine(msg_noCoops_ccp,      rawSize, jdkLilliput_noBase_noCoops,            jdkLilliput_noBase_coops, jdk8_noCoops,         jdk15_noCoops);
            printLine(msg_coops,            rawSize, jdkLilliput_noBase_coops,              jdkLilliput_noBase_coops, jdk8_coops,           jdk15_coops);
            printLine(msg_coops_align16,    rawSize, jdkLilliput_noBase_coops_align16,      jdkLilliput_noBase_coops, jdk8_coops_align16,   jdk15_coops_align16);
            printLine(msg_coops_align32,    rawSize, jdkLilliput_noBase_coops_align32,      jdkLilliput_noBase_coops, jdk8_coops_align32,   jdk15_coops_align32);
            printLine(msg_coops_align64,    rawSize, jdkLilliput_noBase_coops_align64,      jdkLilliput_noBase_coops, jdk8_coops_align64,   jdk15_coops_align64);
            printLine(msg_coops_align128,   rawSize, jdkLilliput_noBase_coops_align128,     jdkLilliput_noBase_coops, jdk8_coops_align128,  jdk15_coops_align128);
            printLine(msg_coops_align256,   rawSize, jdkLilliput_noBase_coops_align256,     jdkLilliput_noBase_coops, jdk8_coops_align256,  jdk15_coops_align256);

        }
        out.println();

        out.println("=== Experimental 64-bit OpenJDK: Lilliput, 64-bit headers, array base improvements");
        out.println();

        long jdkLilliput_base_noCoops =          computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(false,  8,  4, false), 99));
        long jdkLilliput_base_coops =            computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,   8,  4, false), 99));
        long jdkLilliput_base_coops_align16 =    computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,  16,  4, false), 99));
        long jdkLilliput_base_coops_align32 =    computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,  32,  4, false), 99));
        long jdkLilliput_base_coops_align64 =    computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,  64,  4, false), 99));
        long jdkLilliput_base_coops_align128 =   computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 128,  4, false), 99));
        long jdkLilliput_base_coops_align256 =   computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 256,  4, false), 99));

        {
            out.printf("%37s %s%n", "", "Upgrade From:");
            out.printf("%10s, %10s, %10s, %10s, %10s, %10s,     %s%n",
                    "Footprint", "Overhead", "JVM Mode", "JDK < 15", "JDK >= 15", "Lill-64", "Description"
            );

            printLine(msg_noCoops_ccp,      rawSize, jdkLilliput_base_noCoops,          jdkLilliput_base_coops, jdk8_noCoops,           jdk15_noCoops,          jdkLilliput_noBase_noCoops);
            printLine(msg_coops,            rawSize, jdkLilliput_base_coops,            jdkLilliput_base_coops, jdk8_coops,             jdk15_coops,            jdkLilliput_noBase_coops);
            printLine(msg_coops_align16,    rawSize, jdkLilliput_base_coops_align16,    jdkLilliput_base_coops, jdk8_coops_align16,     jdk15_coops_align16,    jdkLilliput_noBase_coops_align16);
            printLine(msg_coops_align32,    rawSize, jdkLilliput_base_coops_align32,    jdkLilliput_base_coops, jdk8_coops_align32,     jdk15_coops_align32,    jdkLilliput_noBase_coops_align32);
            printLine(msg_coops_align64,    rawSize, jdkLilliput_base_coops_align64,    jdkLilliput_base_coops, jdk8_coops_align64,     jdk15_coops_align64,    jdkLilliput_noBase_coops_align64);
            printLine(msg_coops_align128,   rawSize, jdkLilliput_base_coops_align128,   jdkLilliput_base_coops, jdk8_coops_align128,    jdk15_coops_align128,   jdkLilliput_noBase_coops_align128);
            printLine(msg_coops_align256,   rawSize, jdkLilliput_base_coops_align256,   jdkLilliput_base_coops, jdk8_coops_align256,    jdk15_coops_align256,   jdkLilliput_noBase_coops_align256);
        }
        out.println();

        out.println("=== Experimental 64-bit OpenJDK: Lilliput, 32-bit headers");
        out.println();

        long jdkLilliput32_noCoops =        computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(false,  8, 8, true), 99));
        long jdkLilliput32_coops =          computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,   8, 8, true), 99));
        long jdkLilliput32_coops_align16 =  computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,  16, 8, true), 99));
        long jdkLilliput32_coops_align32 =  computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,  32, 8, true), 99));
        long jdkLilliput32_coops_align64 =  computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true,  64, 8, true), 99));
        long jdkLilliput32_coops_align128 = computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 128, 8, true), 99));
        long jdkLilliput32_coops_align256 = computeWithLayouter(data, new HotSpotLayouter(new Model64_Lilliput(true, 256, 8, true), 99));

        {
            out.printf("%37s %s%n", "", "Upgrade From:");
            out.printf("%10s, %10s, %10s, %10s, %10s, %10s, %10s,     %s%n",
                    "Footprint", "Overhead", "JVM Mode", "JDK < 15", "JDK >= 15", "Lill-64", "Lill-64-AB", "Description"
            );

            printLine(msg_noCoops_ccp,    rawSize,  jdkLilliput32_noCoops,        jdkLilliput32_coops, jdk8_noCoops,          jdk15_noCoops,          jdkLilliput_noBase_noCoops,         jdkLilliput_base_noCoops);
            printLine(msg_coops,          rawSize,  jdkLilliput32_coops,          jdkLilliput32_coops, jdk8_coops,            jdk15_coops,            jdkLilliput_noBase_coops,           jdkLilliput_base_coops);
            printLine(msg_coops_align16,  rawSize,  jdkLilliput32_coops_align16,  jdkLilliput32_coops, jdk8_coops_align16,    jdk15_coops_align16,    jdkLilliput_noBase_coops_align16,   jdkLilliput_base_coops_align16);
            printLine(msg_coops_align32,  rawSize,  jdkLilliput32_coops_align32,  jdkLilliput32_coops, jdk8_coops_align32,    jdk15_coops_align32,    jdkLilliput_noBase_coops_align32,   jdkLilliput_base_coops_align32);
            printLine(msg_coops_align64,  rawSize,  jdkLilliput32_coops_align64,  jdkLilliput32_coops, jdk8_coops_align64,    jdk15_coops_align64,    jdkLilliput_noBase_coops_align64,   jdkLilliput_base_coops_align64);
            printLine(msg_coops_align128, rawSize,  jdkLilliput32_coops_align128, jdkLilliput32_coops, jdk8_coops_align256,   jdk15_coops_align128,   jdkLilliput_noBase_coops_align128,  jdkLilliput_base_coops_align128);
            printLine(msg_coops_align256, rawSize,  jdkLilliput32_coops_align256, jdkLilliput32_coops, jdk8_coops_align256,   jdk15_coops_align256,   jdkLilliput_noBase_coops_align256,  jdkLilliput_base_coops_align256);
        }
        out.println();
    }

    private static void printLine(String msg, long rawSize, long value, long... bases) {
        out.printf("%10s, %10s, ", MathUtil.inProperUnits(value), MathUtil.diffPercent(value, rawSize));
        for (long base : bases) {
            out.printf("%10s, ", MathUtil.diffPercent(value, base));
        }
        out.printf("    %s%n", msg);
    }

    private static long computeWithLayouter(Multiset<ClassData> data, Layouter layouter) {
        long size = 0L;
        for (ClassData cd : data.keys()) {
            size += layouter.layout(cd).instanceSize() * data.count(cd);
        }
        return size;
    }

}
