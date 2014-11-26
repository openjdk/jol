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
package org.openjdk.jol;

import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.datamodel.X86_32_DataModel;
import org.openjdk.jol.datamodel.X86_64_COOPS_DataModel;
import org.openjdk.jol.datamodel.X86_64_DataModel;
import org.openjdk.jol.heap.HeapDumpReader;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.util.Multiset;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class MainStringCompress {

    static final DataModel[] DATA_MODELS = new DataModel[]{
            new X86_32_DataModel(),
            new X86_64_DataModel(),
            new X86_64_COOPS_DataModel(),
            new X86_64_COOPS_DataModel(16)
    };


    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: jol-string-compress.jar [heapdump.hprof]*");
            System.exit(1);
        }

        out.printf("%15s, %15s, %15s, %15s, %15s, %15s, %15s, %15s, %15s, %s, %s%n",
                "\"total\"", "\"String\"", "\"String+bool\"", "\"String+oop\"", "\"char[]-2b\"",
                "\"char[]-1b\"", "\"char[]-1b-comp\"", "\"savings(bool)\"", "\"savings(oop)\"", "\"hprof file\"", "\"model\"");

        List<Future<?>> res = new ArrayList<Future<?>>();
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (String arg : args) {
            res.add(service.submit(new Worker(arg)));
        }

        for (Future<?> f : res) {
            try {
                f.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
                // and then ignore
            }
        }

        service.shutdown();
    }

    public static class Worker implements Callable<Object> {
        long stringID;
        int stringValueIdx;
        int stringValueSize;

        final Multiset<Integer> compressibleCharArrays;
        final Multiset<Integer> nonCompressibleCharArrays;
        final String path;

        public Worker(String arg) {
            this.path = arg;
            this.compressibleCharArrays = new Multiset<Integer>();
            this.nonCompressibleCharArrays = new Multiset<Integer>();
        }

        public Object call() throws Exception {
            final Set<Long> referencedArrays = new HashSet<Long>();

            final Map<Long, Boolean> isCompressible = new HashMap<Long, Boolean>();
            final Map<Long, Integer> size = new HashMap<Long, Integer>();

            HeapDumpReader reader = new HeapDumpReader(new File(path)) {
                @Override
                protected void visitClass(long id, String name, List<Integer> oopIdx, int oopSize) {
                    if (name.equals("java/lang/String")) {
                        stringID = id;
                        stringValueIdx = oopIdx.get(0);
                        stringValueSize = oopSize;
                    }
                }

                @Override
                protected void visitInstance(long id, long klassID, byte[] bytes) {
                    if (stringID == 0) {
                        throw new IllegalStateException("java/lang/String was not discovered yet");
                    }
                    if (klassID == stringID) {
                        ByteBuffer wrap = ByteBuffer.wrap(bytes);
                        switch (stringValueSize) {
                            case 4:
                                referencedArrays.add((long) wrap.getInt(stringValueIdx));
                                break;
                            case 8:
                                referencedArrays.add(wrap.getLong(stringValueIdx));
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                    }
                }

                @Override
                protected void visitPrimArray(long id, String typeClass, int count, byte[] bytes) {
                    if (typeClass.equals("char")) {
                        isCompressible.put(id, isCompressible(bytes));
                        size.put(id, count);
                    }
                }
            };

            Multiset<ClassData> data = reader.parse();

            for (Long id : referencedArrays) {
                if (isCompressible.get(id)) {
                    compressibleCharArrays.add(size.get(id));
                } else {
                    nonCompressibleCharArrays.add(size.get(id));
                }
            }

            for (DataModel model : DATA_MODELS) {
                printLine(data, new HotSpotLayouter(model, false, false, false));
            }

            return null;
        }

        private void printLine(Multiset<ClassData> data, Layouter l) {
            long strings = 0;
            long stringsBool = 0;
            long stringsOop = 0;

            long totalFootprint = 0;
            for (ClassData cd : data.keys()) {
                int count = data.count(cd);

                if (cd.name().equals("java/lang/String")) {
                    ClassData mcd = ClassData.parseClass(String.class);
                    strings += l.layout(mcd).instanceSize() * count;

                    ClassData mcdBool = ClassData.parseClass(String.class);
                    mcdBool.addField(FieldData.create("String", "isCompressed", "boolean"));
                    stringsBool += l.layout(mcdBool).instanceSize() * count;

                    ClassData mcdOop = ClassData.parseClass(String.class);
                    mcdOop.addField(FieldData.create("String", "coder", "java/lang/Object"));
                    stringsOop += l.layout(mcdOop).instanceSize() * count;
                } else {
                    totalFootprint += l.layout(cd).instanceSize() * count;
                }
            }

            int compressedBytes = 0;
            int compressibleBytes = 0;
            for (Integer len : compressibleCharArrays.keys()) {
                int count = compressibleCharArrays.count(len);

                ClassData charArr = new ClassData("char[]", "char", len);
                ClassData byteArr = new ClassData("byte[]", "byte", len);

                compressedBytes += l.layout(byteArr).instanceSize() * count;
                compressibleBytes += l.layout(charArr).instanceSize() * count;
            }

            int nonCompressibleBytes = 0;
            for (Integer len : nonCompressibleCharArrays.keys()) {
                ClassData charArr = new ClassData("char[]", "char", len);
                nonCompressibleBytes += l.layout(charArr).instanceSize() * nonCompressibleCharArrays.count(len);
            }

            totalFootprint += strings;

            double savingBool = 100.0 * ((compressibleBytes - compressedBytes) - (stringsBool - strings)) / totalFootprint;
            double savingOop  = 100.0 * ((compressibleBytes - compressedBytes) - (stringsOop - strings))  / totalFootprint;
            out.printf("%15d, %15d, %15d, %15d, %15d, %15d, %15d, %15.3f, %15.3f, \"%s\", \"%s\"%n",
                    totalFootprint, strings, stringsBool, stringsOop, nonCompressibleBytes, compressibleBytes, compressedBytes,
                    savingBool, savingOop, path, l);
        }

        public static boolean isCompressible(byte[] bytes) {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            for (int c = 0; c < bytes.length; c += 2) {
                if ((buf.getShort(c) & 0xFF00) != 0) {
                    return false;
                }
            }
            return true;
        }
    }
}
