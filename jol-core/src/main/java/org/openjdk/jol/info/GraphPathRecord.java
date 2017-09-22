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
package org.openjdk.jol.info;

import org.openjdk.jol.util.ObjectUtils;
import org.openjdk.jol.vm.VM;

/**
 * Object path in object graph.
 *
 * @author Aleksey Shipilev
 */
public class GraphPathRecord {
    private final GraphPathRecord parent;
    private final String path;
    private final int depth;
    private final Object obj;
    private final long size;
    private final long address;
    private String toString;

    GraphPathRecord(GraphPathRecord parent, String path, int depth, Object obj) {
        this.parent = parent;
        this.path = path;
        this.obj = obj;
        this.depth = depth;
        this.size = VM.current().sizeOf(obj);
        this.address = VM.current().addressOf(obj);
    }

    Object obj() {
        return obj;
    }

    public String path() {
        if (parent != null) {
            return parent.path() + path;
        } else {
            return path;
        }
    }

    public Class<?> klass() {
        return obj.getClass();
    }

    public long size() {
        return size;
    }

    public long address() {
        return address;
    }

    public String objToString() {
        String v = toString;
        if (v == null) {
            v = ObjectUtils.safeToString(obj);
            toString = v;
        }
        return v;
    }

    public int depth() {
        return depth;
    }
}
