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
import org.openjdk.jol.util.Multiset;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Experimental heap dump reader
 *
 * @author Aleksey Shipilev
 */
public class HeapDumpReader {

    private final InputStream is;

    private final Map<Long, String> strings;
    private final Map<Long, String> classNames;
    private final Multiset<ClassData> classCounts;
    private final Map<Long, ClassData> classDatas;

    private int idSize;
    private int readBytes;

    private final byte[] buf;
    private final ByteBuffer wrapBuf;

    public HeapDumpReader(File file) throws FileNotFoundException {
        this.is = new BufferedInputStream(new FileInputStream(file), 16*1024*1024);
        this.strings = new HashMap<Long, String>();
        this.classNames = new HashMap<Long, String>();
        this.classCounts = new Multiset<ClassData>();
        this.classDatas = new HashMap<Long, ClassData>();
        this.buf = new byte[4096];
        this.wrapBuf = ByteBuffer.wrap(buf);
    }

    private int read() throws IOException {
        int v = is.read();
        if (v != -1) {
            readBytes++;
            return v;
        } else {
            throw new EOFException();
        }
    }

    private int read(byte[] b, int size) throws IOException {
        int read = is.read(b, 0, size);
        readBytes += read;
        return read;
    }

    public Multiset<ClassData> parse() throws IOException {
        readNullTerminated(); // header

        idSize = read_U4();

        read_U4(); // timestamp, lo
        read_U4(); // timestamp, hi

        while (true) {
            if (is.available() == 0) break;

            int tag = read_U1();
            read_U4(); // relative time
            int len = read_U4();

            int lastCount = readBytes;

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

                    classNames.put(id, strings.get(nameID));
                    break;
                }

                case 0x0C:
                case 0x1C:
                    while (readBytes - lastCount < len) {
                        digestHeapDump();
                    }
                    break;
                default:
                    read_null(len);
            }

            if (readBytes - lastCount != len) {
                throw new IllegalStateException("Expected to read " + len + " bytes, but read " + (readBytes - lastCount) + " bytes");
            }
        }

        return classCounts;
    }

    private void digestHeapDump() throws IOException {
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
                throw new IllegalStateException("Unknown subtag: " + subTag);
        }
    }

    private void digestPrimArray() throws IOException {
        long id = read_ID(); // array id
        read_U4(); // stack trace
        int elements = read_U4();
        int typeClass = read_U1();

        int len = elements * getSize(typeClass);
        byte[] bytes = read_contents(len);

        String typeString = getTypeString(typeClass);

        classCounts.add(new ClassData(typeString + "[]", typeString, elements));

        visitPrimArray(id, typeString, elements, bytes);
    }

    private void digestObjArray() throws IOException {
        read_ID(); // array id
        read_U4(); // stack trace
        int elements = read_U4();
        read_ID(); // type class
        read_null(elements * idSize);

        // assume Object, we don't care about the exact types here
        classCounts.add(new ClassData("Object[]", "Object", elements));
    }

    private void digestInstance() throws IOException {
        long id = read_ID(); // object id
        read_U4(); // stack trace
        long klassID = read_ID();

        classCounts.add(classDatas.get(klassID));

        int instanceBytes = read_U4();

        byte[] bytes = read_contents(instanceBytes);

        visitInstance(id, klassID, bytes);
    }

    private void digestClass() throws IOException {
        long klassID = read_ID();

        String name = classNames.get(klassID);

        ClassData cd = new ClassData(name);
        cd.addSuperClass(name);

        read_U4(); // stack trace

        long superKlassID = read_ID();
        ClassData superCd = classDatas.get(superKlassID);
        if (superCd != null) {
            cd.merge(superCd);
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
        List<Integer> oopIdx = new ArrayList<Integer>();

        int cpInstance = read_U2();
        for (int c = 0; c < cpInstance; c++) {
            long index = read_ID();
            int type = read_U1();

            cd.addField(FieldData.create(name, strings.get(index), getTypeString(type)));
            if (type == 2) {
                oopIdx.add(offset);
            }
            offset += getSize(type);
        }

        classDatas.put(klassID, cd);

        visitClass(klassID, name, oopIdx, idSize);
    }

    private long readValue(int type) throws IOException {
        int size = getSize(type);
        switch (size) {
            case 1:
                return read_U1();
            case 2:
                return read_U2();
            case 4:
                return read_U4();
            case 8:
                return read_U8();
            default:
                throw new IllegalStateException("Unknown size: " + size);
        }
    }

    private int getSize(int type) throws IOException {
        switch (type) {
            case 2: // object
                if (idSize == 4)
                    return 4;
                if (idSize == 8)
                    return 8;
                throw new IllegalStateException();
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
                throw new IllegalStateException("Unknown type: " + type);
        }
    }

    private String getTypeString(int type) throws IOException {
        switch (type) {
            case 2:
                return "Object"; // TODO: Read the exact type;
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
                throw new IllegalStateException("Unknown type: " + type);
        }
    }

    private long read_ID() throws IOException {
        int read = read(buf, idSize);
        if (read == 4)
            return wrapBuf.getInt(0);
        if (read == 8)
            return wrapBuf.getLong(0);
        throw new IllegalStateException("Unable to read " + idSize + " bytes");
    }

    byte[] read_null(int len) throws IOException {
        int rem = len;
        int read;
        do {
            read = read(buf, Math.min(buf.length, rem));
            rem -= read;
        } while (rem > 0);
        return new byte[0];
    }

    byte[] read_contents(int len) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int rem = len;
        int read;
        do {
            read = read(buf, Math.min(buf.length, rem));
            bos.write(buf, 0, read);
            rem -= read;
        } while (rem > 0);
        return bos.toByteArray();
    }

    String readNullTerminated() throws IOException {
        int r;
        StringBuilder sb = new StringBuilder();
        while ((r = read()) != -1) {
            if (r == 0) break;
            sb.append((char) (r & 0xFF));
        }
        return sb.toString();
    }

    String readString(int len) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int l = 0; l < len; l++) {
            int r = read();
            if (r == -1) break;
            sb.append((char) (r & 0xFF));
        }
        return sb.toString();
    }

    long read_U8() throws IOException {
        int read = read(buf, 8);
        if (read == 8) {
            return wrapBuf.getLong(0);
        }
        throw new IllegalStateException("Unable to read 8 bytes");
    }

    int read_U4() throws IOException {
        int read = read(buf, 4);
        if (read == 4) {
            return wrapBuf.getInt(0);
        }
        throw new IllegalStateException("Unable to read 4 bytes");
    }

    int read_U2() throws IOException {
        int read = read(buf, 2);
        if (read == 2) {
            return wrapBuf.getShort(0);
        }
        throw new IllegalStateException("Unable to read 2 bytes");
    }

    int read_U1() throws IOException {
        int read = read(buf, 1);
        if (read == 1) {
            return wrapBuf.get(0);
        }
        throw new IllegalStateException("Unable to read 1 bytes");
    }

    protected void visitInstance(long id, long klassID, byte[] bytes) {

    }

    protected void visitClass(long id, String name, List<Integer> oopIdx, int oopSize) {

    }

    protected void visitPrimArray(long id, String componentType, int count, byte[] bytes) {

    }

}
