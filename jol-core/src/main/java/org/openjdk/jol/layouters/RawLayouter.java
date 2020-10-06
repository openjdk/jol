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
package org.openjdk.jol.layouters;

import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.info.FieldLayout;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Layouter which packs all the fields together, regardless of the alignment.
 *
 * @author Aleksey Shipilev
 */
public class RawLayouter implements Layouter {

    private final DataModel model;

    public RawLayouter(DataModel model) {
        this.model = model;
    }

    @Override
    public ClassLayout layout(ClassData data) {
        SortedSet<FieldLayout> result = new TreeSet<>();

        if (data.isArray()) {
            // special case of arrays
            int base = model.arrayHeaderSize();
            int scale = model.sizeOf(data.arrayComponentType());

            long instanceSize = base + data.arrayLength() * scale;
            result.add(new FieldLayout(FieldData.create(data.arrayClass(), "<elements>", data.arrayComponentType()), base, scale * data.arrayLength()));
            return ClassLayout.createClassLayout(data, result, model.arrayHeaderSize(), instanceSize, false);
        }

        int offset = model.headerSize();
        for (FieldData f : data.fields()) {
            int size = model.sizeOf(f.typeClass());
            result.add(new FieldLayout(f, offset, size));
            offset += size;
        }

        if (result.isEmpty()) {
            return ClassLayout.createClassLayout(data, result, model.headerSize(), model.headerSize(), false);
        } else {
            FieldLayout f = result.last();
            return ClassLayout.createClassLayout(data, result, model.headerSize(), f.offset() + f.size(), false);
        }
    }

    @Override
    public String toString() {
        return "Raw data (" + model + ")";
    }
}
