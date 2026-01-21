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
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.util.Multimap;
import org.openjdk.jol.util.Multiset;

import java.util.*;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class ObjectInternalsEstimates extends ClasspathedOperation {

    @Override
    public String label() {
        return "internals-estimates";
    }

    @Override
    public String description() {
        return "Same as 'internals', but simulate class layout in different VM modes";
    }

    @Override
    protected void runWith(Class<?> klass) {
        List<Layouter> layouters = new ArrayList<>();

        for (DataModel model : EstimatedModels.MODELS_JDK8) {
            layouters.add(new HotSpotLayouter(model, 8));
        }

        for (DataModel model : EstimatedModels.MODELS_JDK8) {
            layouters.add(new HotSpotLayouter(model, 15));
        }

        for (DataModel model : EstimatedModels.MODELS_JDK15) {
            layouters.add(new HotSpotLayouter(model, 15));
            layouters.add(new HotSpotLayouter(model, 23));
            layouters.add(new HotSpotLayouter(model, 25));
        }

        for (DataModel model : EstimatedModels.MODELS_LILLIPUT) {
            layouters.add(new HotSpotLayouter(model, 99));
        }

        Multimap<Wrapper, Layouter> outputs = new Multimap<>();

        for (Layouter l : layouters) {
            ClassLayout cl = ClassLayout.parseClass(klass, l);
            outputs.put(new Wrapper(cl.toPrintable(), cl.instanceSize()), l);
        }

        List<Wrapper> sortedWs = new ArrayList<>(outputs.keys());
        sortedWs.sort(Comparator.comparing(Wrapper::size).reversed());

        for (Wrapper w : sortedWs) {
            for (Layouter l : outputs.get(w)) {
                out.println("***** " + l);
            }
            out.println();
            out.println(w.output);
        }
    }

    private static class Wrapper {
        final String output;
        final long size;

        public Wrapper(String output, long size) {
            this.output = output;
            this.size = size;
        }

        public long size() {
            return size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Wrapper wrapper = (Wrapper) o;
            return Objects.equals(output, wrapper.output);
        }

        @Override
        public int hashCode() {
            return output.hashCode();
        }
    }

}
