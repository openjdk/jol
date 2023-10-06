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
import org.openjdk.jol.util.Multimap;
import org.openjdk.jol.util.Multiset;

import java.io.File;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class HeapDumpStrings implements Operation {

    @Override
    public String label() {
        return "heapdump-strings";
    }

    @Override
    public String description() {
        return "Read a heap dump and look for data that looks duplicated, focusing on Strings";
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

        out.println();
        out.println("Discovering String objects...");
        StringVisitor sv = new StringVisitor();
        HeapDumpReader stringReader = new HeapDumpReader(new File(path), out, sv);
        stringReader.parse();

        out.println();
        out.println("Discovering String contents...");
        StringValueVisitor svv = new StringValueVisitor(sv.valuesToStrings());
        HeapDumpReader stringValueReader = new HeapDumpReader(new File(path), out, svv);
        Multiset<ClassData> data = stringValueReader.parse();

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

        svv.computeDuplicates(out, layouter);
        out.println();
    }

    public static class StringContents {
        private final int length;
        private final String componentType;
        private final byte[] contents;
        private final long hash;

        public StringContents(int length, String componentType, byte[] contents) {
            this.length = length;
            this.componentType = componentType;
            this.contents = Arrays.copyOf(contents, Math.min(contents.length, 32));
            this.hash = byteArrayHashCode(contents);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StringContents that = (StringContents) o;
            return length == that.length && hash == that.hash && componentType.equals(that.componentType);
        }

        @Override
        public int hashCode() {
            return (int) ((hash >> 32) ^ (hash));
        }

        public Object value() {
            if (componentType.equals("byte")) {
                // Boldly assume Latin1 encoding
                return new String(contents, StandardCharsets.ISO_8859_1);
            } else if (componentType.equals("char")) {
                return new String(contents, StandardCharsets.UTF_16);
            } else {
                return "N/A";
            }
        }
    }

    public static long byteArrayHashCode(byte[] src) {
        long result = 1;
        for (byte e : src) {
            result = 31 * result + e;
        }
        return result;
    }

    public static class StringVisitor extends HeapDumpReader.Visitor {
        private final Multimap<Long, Long> valuesToStrings = new Multimap<>();

        private long stringID;
        private int stringValueOffset;
        private int stringValueSize;

        @Override
        public void visitInstance(long id, long klassID, byte[] bytes, String name) {
            if (klassID == stringID) {
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                long valueId;
                switch (stringValueSize) {
                    case 4:
                        valueId = bb.getInt(stringValueOffset);
                        break;
                    case 8:
                        valueId = bb.getLong(stringValueOffset);
                        break;
                    default:
                        throw new IllegalStateException("Cannot handle string value size: " + stringValueSize);
                }
                valuesToStrings.put(valueId, id);
            }
        }

        @Override
        public void visitClass(long id, String name, List<Integer> oopIdx, int oopSize) {
            if (name.equals("java.lang.String")) {
                stringID = id;
                if (oopIdx.size() == 1) {
                    stringValueOffset = oopIdx.get(0);
                    stringValueSize = oopSize;
                } else {
                    throw new IllegalStateException("String has more than one reference field");
                }
            }
        }

        public Multimap<Long, Long> valuesToStrings() {
            return valuesToStrings;
        }
    }

    public static class StringValueVisitor extends HeapDumpReader.Visitor {
        private final Multimap<Long, Long> valuesToStrings;
        private final Multiset<StringContents> contents = new Multiset<>();

        public StringValueVisitor(Multimap<Long, Long> valuesToStrings) {
            this.valuesToStrings = valuesToStrings;
        }

        @Override
        public void visitArray(long id, String componentType, int count, byte[] bytes) {
            if (valuesToStrings.contains(id)) {
                contents.add(new StringContents(count, componentType, bytes));
            }
        }

        public void computeDuplicates(PrintStream ps, Layouter layouter) {
            long stringSize = layouter.layout(ClassData.parseClass(String.class)).instanceSize();

            ASCIITable table = new ASCIITable(
                    false,
                    "=== Duplicate Strings\n" +
                    "  DUPS: Number of duplicated String instances\n" +
                    "  SIZE (V): Savings due to String.value dedup (automatic by GC)\n" +
                    "  SIZE (S+V): Savings due to entire String dedup (manual)",
                    "DUPS", "SIZE (V)", "SIZE (S+V)", "VALUE");

            for (StringContents sc : contents.keys()) {
                long count = contents.count(sc) - 1;
                if (count > 0) {
                    ClassData cd = new ClassData(sc.componentType + "[]", sc.componentType, sc.length);
                    long size = layouter.layout(cd).instanceSize();
                    table.addLine(
                            sc.value() + ((sc.length > 32) ? "... (" + sc.length + " chars)" : ""),
                            count,
                            count * size,
                            count * (size + stringSize)
                    );
                }
            }

            table.print(ps, 0);
            table.print(ps, 1);
            table.print(ps, 2);
        }
    }

}
