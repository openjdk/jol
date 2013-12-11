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
package org.openjdk.jol.samples;

import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.util.VMSupport;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.lang.System.out;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_16_AL_LL {

    /*
     * The example of traversing the reachability graphs.
     *
     * In addition to introspecting the object internals, we can also
     * see the object externals, that is, the objects referenced from the
     * object in question. There are multiple ways to illustrate this,
     * the summary table seems to work well.
     *
     * In this example, you can clearly see the difference between
     * the shadow heap (i.e. space taken by the reachable objects)
     * for ArrayList and LinkedList.
     */

    public static void main(String[] args) throws Exception {
        out.println(VMSupport.vmDetails());

        List<Integer> al = new ArrayList<Integer>();
        List<Integer> ll = new LinkedList<Integer>();

        for (int i = 0; i < 1000; i++) {
            al.add(i);
            ll.add(i);
        }

        PrintWriter pw = new PrintWriter(out);
        pw.println(GraphLayout.parseInstance(al).toFootprint());
        pw.println(GraphLayout.parseInstance(ll).toFootprint());
        pw.close();
    }

}
