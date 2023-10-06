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
package org.openjdk.jol.heap;

import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.util.ClassUtils;
import org.openjdk.jol.util.Multimap;
import org.openjdk.jol.util.Multiset;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Experimental heap dump reader
 *
 * @author Aleksey Shipilev
 */
public class HeapDumpReader {

    private static final int GZIP_BUF_SIZE = 16 * 1024 * 1024;
    private static final int READ_BUF_SIZE = 16 * 1024 * 1024;

    private final InputStream is;

    private final Map<Long, String> strings;
    private final Map<Long, String> classNames;
    private final Multimap<Long, FieldData> classFields;
    private final Multiset<Long> classCounts;
    private final Multiset<ClassData> arrayCounts;
    private final Map<Long, Long> classSupers;
    private final File file;
    private final PrintStream verboseOut;
    private final Visitor visitor;

    private int idSize;
    private long readBytes;

    private final byte[] buf;
    private final ByteBuffer wrapBuf;
    private String header;

    private static class SrcDstTypes {
        final ClassData src;
        final ClassData dst;

        private SrcDstTypes(ClassData src, ClassData dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SrcDstTypes that = (SrcDstTypes) o;
            return src.equals(that.src) && dst.equals(that.dst);
        }

        @Override
        public int hashCode() {
            return Objects.hash(src, dst);
        }
    }

    public HeapDumpReader(File file, PrintStream verboseOut, Visitor visitor) throws IOException {
        this.file = file;
        this.verboseOut = verboseOut;
        this.visitor = visitor;
        if (file.getName().endsWith(".gz")) {
            this.is = new UnsyncBufferedInputStream(new GZIPInputStream(new FileInputStream(file), GZIP_BUF_SIZE), READ_BUF_SIZE);
        } else {
            this.is = new UnsyncBufferedInputStream(new FileInputStream(file), READ_BUF_SIZE);
        }
        this.strings = new HashMap<>();
        this.classNames = new HashMap<>();
        this.classCounts = new Multiset<>();
        this.classFields = new Multimap<>();
        this.arrayCounts = new Multiset<>();
        this.classSupers = new HashMap<>();
        this.buf = new byte[32*1024];
        this.wrapBuf = ByteBuffer.wrap(buf);
    }

    private int read() throws HeapDumpException {
        try {
            int v = is.read();
            if (v != -1) {
                readBytes++;
                return v;
            } else {
                throw new HeapDumpException(errorMessage("EOF"));
            }
        } catch (IOException e) {
            throw new HeapDumpException(errorMessage(e.getMessage()));
        }
    }

    private int read(byte[] b, int size) throws HeapDumpException {
        try {
            int read = is.read(b, 0, size);
            readBytes += read;
            return read;
        } catch (IOException e) {
            throw new HeapDumpException(errorMessage(e.getMessage()));
        }
    }

    public Multiset<ClassData> parse() throws IOException, HeapDumpException {
        header = readNullTerminated();

        idSize = (int) read_U4(); // always fits

        read_U4(); // timestamp, lo
        read_U4(); // timestamp, hi

        long lastPrint = 0L;
        final long printEach = 256L * 1024 * 1024;

        if (verboseOut != null) {
            verboseOut.print("Read progress: ");
            verboseOut.flush();
        }

        while (true) {
            if ((verboseOut != null) && (readBytes - lastPrint > printEach)) {
                verboseOut.print(readBytes / 1000 / 1000 + "M... ");
                verboseOut.flush();
                lastPrint = readBytes;
            }

            int tag;
            try {
                tag = read_U1();
            } catch (HeapDumpException e) {
                // EOF, break out
                break;
            }

            read_U4(); // relative time
            long len = read_U4();

            long lastCount = readBytes;

            switch (tag) {
                case 0x01: {
                    long id = read_ID();
                    String s = readString(len - idSize);
                    strings.put(id, s);
                    break;
                }

                case 0x02: {
                    read_U4(); // serial
                    long id = read_ID();
                    read_U4(); // stack trace
                    long nameID = read_ID();

                    classNames.put(id, ClassUtils.binaryToHuman(strings.get(nameID)));
                    break;
                }

                case 0x0C:
                case 0x1C:
                    while (readBytes - lastCount < len) {
                        digestHeapDump();
                    }
                    break;
                default:
                    skipContents(len);
            }

            if (readBytes - lastCount != len) {
                throw new HeapDumpException(errorMessage("Expected to read " + len + " bytes, but read " + (readBytes - lastCount) + " bytes"));
            }
        }

        // Post-process supers: merge all fields datas up the class hierarchy.
        Map<Long, ClassData> classDatas = new HashMap<>();

        for (Long klassId : classFields.keys()) {
            String name = classNames.get(klassId);
            ClassData cd = new ClassData(name);

            Long id = klassId;
            while (id != null) {
                cd.addSuperClass(classNames.get(id));
                for (FieldData fd : classFields.get(id)) {
                    cd.addField(fd);
                }
                id = classSupers.get(id);
            }
            classDatas.put(klassId, cd);
            if (visitor != null) {
                visitor.visitClassData(name, cd);
            }
        }

        // Fix up superclasses for HotspotLayouter to work well.
        for (Long klassId : classDatas.keySet()) {
            Long key = classSupers.get(klassId);
            if (key != null) {
                ClassData cd = classDatas.get(klassId);
                ClassData superCd = classDatas.get(key);
                if (superCd == null) {
                    throw new IllegalStateException("Parser error: no super class data for " + cd.name() + " (" + key + ")");
                }
                cd.addSuperClassData(superCd);
            }
        }

        // Compute final class counts.
        Multiset<ClassData> finalClassCounts = new Multiset<>();
        for (ClassData cd : arrayCounts.keys()) {
            finalClassCounts.add(cd, arrayCounts.count(cd));
        }
        for (Long id : classDatas.keySet()) {
            ClassData cd = classDatas.get(id);
            finalClassCounts.add(cd, classCounts.count(id));
        }

        if (verboseOut != null) {
            verboseOut.println("DONE");
        }

        return finalClassCounts;
    }

    private void digestHeapDump() throws HeapDumpException {
        int subTag = read_U1();
        switch (subTag) {
            case 0x01:
                read_ID();
                read_ID();
                return;
            case 0x02:
                read_ID();
                read_U4();
                read_U4();
                return;
            case 0x03:
                read_ID();
                read_U4();
                read_U4();
                return;
            case 0x04:
                read_ID();
                read_U4();
                return;
            case 0x05:
                read_ID();
                return;
            case 0x06:
                read_ID();
                read_U4();
                return;
            case 0x07:
                read_ID();
                return;
            case 0x08:
                read_ID();
                read_U4();
                read_U4();
                return;
            case 0x20:
                digestClass();
                return;
            case 0x21:
                digestInstance();
                return;
            case 0x22:
                digestObjArray();
                return;
            case 0x23:
                digestPrimArray();
                return;
            default:
                throw new HeapDumpException(errorMessage(String.format("Unknown heap dump subtag 0x%x", subTag)));
        }
    }

    private void digestPrimArray() throws HeapDumpException {
        long id = read_ID(); // array id
        read_U4(); // stack trace
        int elements = (int) read_U4(); // always fits
        int typeClass = read_U1();

        String typeString = getTypeString(typeClass);
        ClassData thisCD = new ClassData(typeString + "[]", typeString, elements);
        arrayCounts.add(thisCD);

        long len = (long) elements * getSize(typeClass);
        if (visitor != null) {
            byte[] bytes = readContents(len);
            visitor.visitArray(id, typeString, elements, bytes);
        } else {
            skipContents(len);
        }
    }

    private void digestObjArray() throws HeapDumpException {
        long id = read_ID(); // array id
        read_U4(); // stack trace
        int elements = (int) read_U4(); // always fits
        long klassId = read_ID(); // array class

        String name = classNames.get(klassId);

        // Assume Object as component type, the name of the actual class
        // is what we want for the printouts.
        ClassData thisCD = new ClassData(name, "Object", elements);
        arrayCounts.add(new ClassData(name, "Object", elements));

        long len = (long) elements * idSize;
        if (visitor != null) {
            byte[] bytes = readContents(len);
            visitor.visitArray(id, "Object", elements, bytes);
        } else {
            skipContents(len);
        }
    }

    private void digestInstance() throws HeapDumpException {
        long id = read_ID(); // object id
        read_U4(); // stack trace
        long klassID = read_ID();

        classCounts.add(klassID);

        int instanceBytes = (int) read_U4(); // always fits

        if (visitor != null) {
            byte[] bytes = readContents(instanceBytes);
            String name = classNames.get(klassID);
            visitor.visitInstance(id, klassID, bytes, name);
        } else {
            skipContents(instanceBytes);
        }
    }

    private void digestClass() throws HeapDumpException {
        long klassID = read_ID();

        String name = classNames.get(klassID);

        read_U4(); // stack trace

        long superKlassID = read_ID();
        if (superKlassID != 0 && classSupers.put(klassID, superKlassID) != null) {
            throw new HeapDumpException("Format error: duplicate class " + name);
        }

        read_ID(); // class loader
        read_ID(); // signers
        read_ID(); // protection domain
        read_ID(); // reserved
        read_ID(); // reserved
        read_U4(); // instance size

        int cpCount = read_U2();
        for (int c = 0; c < cpCount; c++) {
            read_U2(); // cp index
            int type = read_U1(); // cp type
            readValue(type); // value
        }

        int cpStatics = read_U2();
        for (int c = 0; c < cpStatics; c++) {
            read_ID(); // index
            int type = read_U1(); // type
            readValue(type); // value
        }

        int offset = 0;
        List<Integer> oopIdx = new ArrayList<>();

        int cpInstance = read_U2();
        for (int c = 0; c < cpInstance; c++) {
            long index = read_ID();
            int type = read_U1();

            classFields.put(klassID, FieldData.create(name, strings.get(index), getTypeString(type)));
            if (type == 2) {
                oopIdx.add(offset);
            }
            offset += getSize(type);
        }
        if (cpInstance == 0) {
            classFields.putEmpty(klassID);
        }

        if (visitor != null) {
            visitor.visitClass(klassID, name, oopIdx, idSize);
        }
    }

    private long readValue(int type) throws HeapDumpException {
        switch (type) {
            case 2: // object
                if (idSize == 4) {
                    return read_U4();
                }
                if (idSize == 8) {
                    return read_U8();
                }
                throw new HeapDumpException("Illegal ID size");

            case 4: // boolean
            case 8: // byte
                return (byte) read_U1();
            case 9: // short
            case 5: // char
                return (short) read_U2();
            case 10: // int
            case 6: // float
                return (int) read_U4();

            case 7: // double
            case 11: // long
                return read_U8();

            default:
                throw new HeapDumpException("Unknown type: " + type);
        }
    }

    private int getSize(int type) throws HeapDumpException {
        switch (type) {
            case 2: // object
                if (idSize == 4) {
                    return 4;
                }
                if (idSize == 8) {
                    return 8;
                }
                throw new HeapDumpException("Illegal ID size");
            case 4: // boolean
            case 8: // byte
                return 1;

            case 9: // short
            case 5: // char
                return 2;

            case 10: // int
            case 6: // float
                return 4;

            case 7: // double
            case 11: // long
                return 8;

            default:
                throw new HeapDumpException("Unknown type: " + type);
        }
    }

    private String getTypeString(int type) throws HeapDumpException {
        if (type == 2) {
            return "Object"; // TODO: Read the exact type;
        }

        return getPrimitiveTypeString(type);
    }

    private String getPrimitiveTypeString(int type) throws HeapDumpException {
        switch (type) {
            case 4:
                return "boolean";
            case 8:
                return "byte";
            case 9:
                return "short";
            case 5:
                return "char";
            case 10:
                return "int";
            case 6:
                return "float";
            case 7:
                return "double";
            case 11:
                return "long";
            default:
                throw new HeapDumpException("Unknown type: " + type);
        }
    }

    private long read_ID() throws HeapDumpException {
        int read = read(buf, idSize);
        if (read == 4) {
            return ((long)wrapBuf.getInt(0) & 0xFFFFFFFFL);
        }
        if (read == 8) {
            return wrapBuf.getLong(0);
        }
        throw new HeapDumpException("Unable to read " + idSize + " bytes");
    }

    void skipContents(long len) throws HeapDumpException {
        long rem = len;
        int read;
        do {
            int toRead = (int) Math.min(buf.length, rem); // always fits into buf.length
            read = read(buf, toRead);
            rem -= read;
        } while (rem > 0);
    }

    byte[] readContents(long len) throws HeapDumpException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        long rem = len;
        int read;
        do {
            int toRead = (int) Math.min(buf.length, rem); // always fits into buf.length
            read = read(buf, toRead);
            bos.write(buf, 0, read);
            rem -= read;
        } while (rem > 0);
        return bos.toByteArray();
    }

    String readNullTerminated() throws HeapDumpException {
        int r;
        StringBuilder sb = new StringBuilder();
        while ((r = read()) != -1) {
            if (r == 0) {
                break;
            }
            sb.append((char) (r & 0xFF));
        }
        return sb.toString();
    }

    String readString(long len) throws HeapDumpException {
        StringBuilder sb = new StringBuilder();
        for (long l = 0; l < len; l++) {
            int r = read();
            if (r == -1) {
                break;
            }
            sb.append((char) (r & 0xFF));
        }
        return sb.toString();
    }

    long read_U8() throws HeapDumpException {
        int read = read(buf, 8);
        if (read == 8) {
            return wrapBuf.getLong(0);
        }
        throw new HeapDumpException(errorMessage("Unable to read 8 bytes"));
    }

    long read_U4() throws HeapDumpException {
        int read = read(buf, 4);
        if (read == 4) {
            return ((long)wrapBuf.getInt(0) & 0xFFFFFFFFL);
        }
        throw new HeapDumpException(errorMessage("Unable to read 4 bytes"));
    }

    int read_U2() throws HeapDumpException {
        int read = read(buf, 2);
        if (read == 2) {
            return ((int)wrapBuf.getShort(0) & 0xFFFF);
        }
        throw new HeapDumpException(errorMessage("Unable to read 2 bytes"));
    }

    int read_U1() throws HeapDumpException {
        int read = read(buf, 1);
        if (read == 1) {
            return ((int)wrapBuf.get(0) & 0xFF);
        }
        throw new HeapDumpException(errorMessage("Unable to read 1 bytes"));
    }

    private String errorMessage(String message) throws HeapDumpException {
        return String.format("%s at offset 0x%x in %s (%s)", message, readBytes, file, header);
    }

    public static class Visitor {
        public void visitInstance(long id, long klassID, byte[] bytes, String name) {
            // Do nothing.
        }

        public void visitClass(long id, String name, List<Integer> oopIdx, int oopSize) {
            // Do nothing.
        }

        public void visitArray(long id, String componentType, int count, byte[] bytes) {
            // Do nothing.
        }

        public void visitClassData(String name, ClassData cd) {
            // Do nothing.
        }
    }

    public static class MultiplexingVisitor extends Visitor {
        private final List<Visitor> visitors = new ArrayList<>();
        public void add(Visitor v) {
            visitors.add(v);
        }

        @Override
        public void visitInstance(long id, long klassID, byte[] bytes, String name) {
            for (Visitor v : visitors) {
                v.visitInstance(id, klassID, bytes, name);
            }
        }

        @Override
        public void visitClass(long id, String name, List<Integer> oopIdx, int oopSize) {
            for (Visitor v : visitors) {
                v.visitClass(id, name, oopIdx, oopSize);
            }
        }

        @Override
        public void visitArray(long id, String componentType, int count, byte[] bytes) {
            for (HeapDumpReader.Visitor v : visitors) {
                v.visitArray(id, componentType, count, bytes);
            }
        }

        @Override
        public void visitClassData(String name, ClassData cd) {
            for (HeapDumpReader.Visitor v : visitors) {
                v.visitClassData(name, cd);
            }
        }
    }


    static class UnsyncBufferedInputStream extends BufferedInputStream {
        public UnsyncBufferedInputStream(InputStream in, int bufSize) {
            super(in, bufSize);
        }

        @Override
        public int read() throws IOException {
            if (pos >= count) {
                // Let superclass handle buffers
                return super.read();
            }
            return buf[pos++] & 0xFF;
        }
    }

}
