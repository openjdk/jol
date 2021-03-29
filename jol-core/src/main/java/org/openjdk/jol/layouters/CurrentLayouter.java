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

import org.openjdk.jol.datamodel.ModelVM;
import org.openjdk.jol.datamodel.DataModel;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.info.FieldLayout;
import org.openjdk.jol.util.MathUtil;
import org.openjdk.jol.vm.VM;
import org.openjdk.jol.vm.VirtualMachine;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The layouter getting the actual VM layout.
 *
 * @author Aleksey Shipilev
 */
public class CurrentLayouter implements Layouter {

    static final DataModel CURRENT = new ModelVM();

    @Override
    public ClassLayout layout(ClassData data) {
        VirtualMachine vm = VM.current();

        if (data.isArray()) {
            // special case of arrays
            int base = vm.arrayBaseOffset(data.arrayComponentType());
            int scale = vm.arrayIndexScale(data.arrayComponentType());

            long instanceSize = MathUtil.align(base + data.arrayLength() * scale, vm.objectAlignment());

            SortedSet<FieldLayout> result = new TreeSet<>();
            result.add(new FieldLayout(FieldData.create(data.arrayClass(), "<elements>", data.arrayComponentType()), base, scale * data.arrayLength()));
            return ClassLayout.create(data, result, CURRENT, instanceSize, false);
        }

        Collection<FieldData> fields = data.fields();

        SortedSet<FieldLayout> result = new TreeSet<>();
        for (FieldData f : fields) {
            result.add(new FieldLayout(f, vm.fieldOffset(f.refField()), vm.sizeOfField(f.typeClass())));
        }

        long instanceSize;
        if (result.isEmpty()) {
            instanceSize = vm.objectHeaderSize();
        } else {
            FieldLayout f = result.last();
            instanceSize = f.offset() + f.size();
            // TODO: This calculation is incorrect if there is a trailing @Contended field, or the instance is @Contended
        }
        instanceSize = MathUtil.align(instanceSize, vm.objectAlignment());
        return ClassLayout.create(data, result, CURRENT, instanceSize, true);
    }

    @Override
    public String toString() {
        return "Current VM Layout";
    }

}
