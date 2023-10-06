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
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.util.ASCIITable;
import org.openjdk.jol.util.Multiset;

import java.io.*;
import java.util.*;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class HeapDumpDuplicates implements Operation {

    @Override
    public String label() {
        return "heapdump-duplicates";
    }

    @Override
    public String description() {
        return "Read a heap dump and look for data that looks duplicated";
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

        HeapDumpReader.MultiplexingVisitor mv = new HeapDumpReader.MultiplexingVisitor();

        InstanceVisitor iv = new InstanceVisitor();
        mv.add(iv);

        ArrayContentsVisitor av = new ArrayContentsVisitor();
        mv.add(av);

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

        List<ExcessRow> excesses = new ArrayList<>();
        excesses.addAll(iv.compute(layouter));
        excesses.addAll(av.compute(layouter));
        excesses.sort((c1, c2) -> Long.compare(c2.excessV, c1.excessV));

        ASCIITable table = new ASCIITable(
                true,
                "=== Potential Duplication Candidates",
                "DUPS", "SUM SIZE", "CLASS");

        for (ExcessRow s : excesses) {
            table.addLine(s.name, s.excessC, s.excessV);
        }
        table.print(out, 1);

        for (ExcessRow s : excesses) {
            out.println(s.fullTable);
        }
    }

    public static class InstanceContents {
        private final long contents;
        private final boolean contentsIsHash;
        private final boolean contentsIsZero;
        private final byte contentsLen;

        public InstanceContents(byte[] contents) {
            if (contents.length <= 8) {
                this.contents = bytePrefixToLong(contents);
                this.contentsIsZero = byteArrayZero(contents);
                this.contentsIsHash = false;
                this.contentsLen = (byte) contents.length;
            } else {
                this.contents = byteArrayHashCode(contents);
                this.contentsIsZero = byteArrayZero(contents);
                this.contentsIsHash = true;
                this.contentsLen = -1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InstanceContents that = (InstanceContents) o;
            return contents == that.contents && contentsIsHash == that.contentsIsHash;
        }

        @Override
        public int hashCode() {
            return (int) ((contents >> 32) ^ (contents));
        }

        public String value() {
            if (contentsIsHash) {
                if (contentsIsZero) {
                    return "{ 0 }";
                } else {
                    return "(hash: " + Long.toHexString(contents) + ")";
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            switch (contentsLen) {
                case 1:
                    sb.append(contents & 0xFFL);
                    break;
                case 2:
                    sb.append(contents & 0xFFFFL);
                    break;
                case 4:
                    sb.append(contents & 0xFFFF_FFFFL);
                    break;
                case 8:
                    sb.append(contents);
                    break;
            }
            sb.append(" }");

            return sb.toString();
        }
    }

    public static class HashedArrayContents {
        private final int length;
        private final String componentType;
        private final long contents;
        private final boolean contentsIsHash;
        private final boolean contentsIsZero;

        public HashedArrayContents(int length, String componentType, byte[] contents) {
            this.length = length;
            this.componentType = componentType;
            if (contents.length <= 8) {
                this.contents = bytePrefixToLong(contents);
                this.contentsIsHash = false;
                this.contentsIsZero = byteArrayZero(contents);
            } else {
                this.contents = byteArrayHashCode(contents);
                this.contentsIsHash = true;
                this.contentsIsZero = byteArrayZero(contents);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HashedArrayContents that = (HashedArrayContents) o;
            return length == that.length && contents == that.contents && contentsIsHash == that.contentsIsHash && componentType.equals(that.componentType);
        }

        @Override
        public int hashCode() {
            return (int) ((contents >> 32) ^ (contents));
        }

        private int unitSize() {
            switch (componentType) {
                case "boolean":
                case "byte":
                    return 1;
                case "short":
                case "char":
                    return 2;
                case "int":
                case "float":
                    return 4;
                case "double":
                case "long":
                    return 8;
                default:
                    return 4;
            }
        }

        public String value() {
            if (contentsIsHash) {
                if (contentsIsZero) {
                    return componentType + "[" + length + "] { 0, ..., 0 }";
                } else {
                    return componentType + "[" + length + "] (hash: " + Long.toHexString(contents) + ")";
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append(componentType);
            sb.append("[");
            sb.append(length);
            sb.append("] { ");
            switch (unitSize()) {
                case 1:
                    switch (length) {
                        case 8:
                            sb.append((contents >> 56) & 0xFF);
                            sb.append(", ");
                            sb.append((contents >> 48) & 0xFF);
                            sb.append(", ");
                            sb.append((contents >> 40) & 0xFF);
                            sb.append(", ");
                            sb.append((contents >> 32) & 0xFF);
                            sb.append(", ");
                        case 4:
                            sb.append((contents >> 24) & 0xFF);
                            sb.append(", ");
                            sb.append((contents >> 16) & 0xFF);
                            sb.append(", ");
                        case 2:
                            sb.append((contents >> 8) & 0xFF);
                            sb.append(", ");
                        case 1:
                            sb.append((contents >> 0) & 0xFF);
                    }
                    break;
                case 2:
                    switch (length) {
                        case 4:
                            sb.append((contents >> 48) & 0xFFFF);
                            sb.append(", ");
                            sb.append((contents >> 32) & 0xFFFF);
                            sb.append(", ");
                        case 2:
                            sb.append((contents >> 16) & 0xFFFF);
                            sb.append(", ");
                        case 1:
                            sb.append((contents >> 0) & 0xFFFF);
                    }
                    break;
                case 4:
                    switch (length) {
                        case 2:
                            sb.append((contents >> 32) & 0xFFFF_FFFFL);
                            sb.append(", ");
                        case 1:
                            sb.append((contents >> 0) & 0xFFFF_FFFFL);
                    }
                    break;
                case 8:
                    sb.append(contents);
                    break;
            }
            sb.append(" }");

            return sb.toString();
        }
    }

    private static long bytePrefixToLong(byte[] src) {
        int limit = Math.min(src.length, 8);
        long res = 0;
        for (int c = 0; c < limit; c++) {
            res = (res << 8) + (src[c] & 0xFF);
        }
        return res;
    }

    public static long byteArrayHashCode(byte[] src) {
        long result = 1;
        for (byte e : src) {
            result = 31 * result + e;
        }
        return result;
    }

    public static boolean byteArrayZero(byte[] src) {
        for (byte e : src) {
            if (e != 0) {
                return false;
            }
        }
        return true;
    }

    public static class InstanceVisitor extends HeapDumpReader.Visitor {
        private final Map<String, Multiset<InstanceContents>> contents = new HashMap<>();
        private final Map<String, ClassData> classDatas = new HashMap<>();

        @Override
        public void visitInstance(long id, long klassID, byte[] bytes, String name) {
            Multiset<InstanceContents> conts = contents.get(name);
            if (conts == null) {
                conts = new Multiset<>();
                contents.put(name, conts);
            } else {
                conts.pruneForSize(1_000_000);
            }
            conts.add(new InstanceContents(bytes));
        }

        @Override
        public void visitClassData(String name, ClassData cd) {
            classDatas.put(name, cd);
        }

        public List<ExcessRow> compute(Layouter layouter) {
            List<ExcessRow> excesses = new ArrayList<>();
            for (String name : contents.keySet()) {
                Multiset<InstanceContents> ics = contents.get(name);

                boolean hasExcess = false;
                for (InstanceContents ba : ics.keys()) {
                    long count = ics.count(ba);
                    if (count > 1) {
                        hasExcess = true;
                        break;
                    }
                }

                if (!hasExcess) {
                    continue;
                }

                ClassData cd = classDatas.get(name);
                if (cd == null) {
                    throw new IllegalStateException("Internal error: no class data for " + name);
                }

                long intSize = layouter.layout(cd).instanceSize();

                ASCIITable table = new ASCIITable(
                        true,
                        "=== " + cd.name() + " Potential Duplicates\n" +
                        "  DUPS: Number of instances with same data\n" +
                        "  SIZE: Total size taken by duplicate instances",
                        "DUPS", "SIZE", "VALUE");

                long excessV = 0;
                long excessC = 0;

                for (InstanceContents ic : ics.keys()) {
                    long count = ics.count(ic) - 1;
                    if (count > 0) {
                        long sumV = count * intSize;
                        table.addLine(ic.value(), count, sumV);
                        excessV += sumV;
                        excessC += count;
                    }
                }

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                table.print(pw, 1);
                excesses.add(new ExcessRow(excessC, excessV, cd.name(), sw.toString()));
            }
            return excesses;
        }
    }

    public static class ArrayContentsVisitor extends HeapDumpReader.Visitor {
        private final Map<String, Multiset<HashedArrayContents>> arrayContents = new HashMap<>();

        @Override
        public void visitArray(long id, String componentType, int count, byte[] bytes) {
            Multiset<HashedArrayContents> conts = arrayContents.get(componentType);
            if (conts == null) {
                conts = new Multiset<>();
                arrayContents.put(componentType, conts);
            } else {
                conts.pruneForSize(1_000_000);
            }
            conts.add(new HashedArrayContents(count, componentType, bytes));
        }

        public List<ExcessRow> compute(Layouter layouter) {
            List<ExcessRow> excesses = new ArrayList<>();
            for (String componentType : arrayContents.keySet()) {
                Multiset<HashedArrayContents> hacs = arrayContents.get(componentType);

                boolean hasExcess = false;
                for (HashedArrayContents ba : hacs.keys()) {
                    long count = hacs.count(ba);
                    if (count > 1) {
                        hasExcess = true;
                        break;
                    }
                }

                if (!hasExcess) {
                    continue;
                }

                ASCIITable table = new ASCIITable(
                        true,
                        "=== " + componentType + "[] Potential Duplicates\n" +
                        "  DUPS: Number of instances with same data\n" +
                        "  SIZE: Total size taken by duplicate instances",
                        "DUPS", "SIZE", "VALUE");

                long excessV = 0;
                long excessC = 0;
                for (HashedArrayContents hac : hacs.keys()) {
                    long count = hacs.count(hac) - 1;
                    if (count > 0) {
                        ClassData cd = new ClassData(componentType + "[]", componentType, hac.length);
                        long intSize = layouter.layout(cd).instanceSize();
                        long sumV = count * intSize;
                        table.addLine(hac.value(), count, sumV);
                        excessV += sumV;
                        excessC += count;
                    }
                }

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                table.print(pw, 1);
                excesses.add(new ExcessRow(excessC, excessV, componentType + "[]", sw.toString()));
            }
            return excesses;
        }

    }

    private static class ExcessRow {
        final long excessC;
        final long excessV;
        final String name;
        final String fullTable;

        public ExcessRow(long excessC, long excessV, String name, String fullTable) {
            this.excessC = excessC;
            this.excessV = excessV;
            this.name = name;
            this.fullTable = fullTable;
        }
    }

}
