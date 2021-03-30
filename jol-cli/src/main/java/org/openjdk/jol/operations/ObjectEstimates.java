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
package org.openjdk.jol.operations;

import org.openjdk.jol.datamodel.*;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.HotSpotLayouter;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class ObjectEstimates extends ClasspathedOperation {

    @Override
    public String label() {
        return "estimates";
    }

    @Override
    public String description() {
        return "Simulate the class layout in different VM modes.";
    }

    static final DataModel[] DATA_MODELS = new DataModel[]{
            new Model32(),
            new Model64(),
            new Model64_COOPS_CCPS(),
            new Model64_COOPS_CCPS(16),
            new Model64_CCPS(),
            new Model64_CCPS(16),
    };

    @Override
    protected void runWith(Class<?> klass) {
        for (DataModel model : DATA_MODELS) {
            out.println("***** " + model.toString());
            out.println(ClassLayout.parseClass(klass, new HotSpotLayouter(model)).toPrintable());
        }
    }

}
