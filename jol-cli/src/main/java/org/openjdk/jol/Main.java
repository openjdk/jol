/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jol.operations.HeapDump;
import org.openjdk.jol.operations.ObjectEstimates;
import org.openjdk.jol.operations.ObjectExternals;
import org.openjdk.jol.operations.ObjectFootprint;
import org.openjdk.jol.operations.ObjectIdealPacking;
import org.openjdk.jol.operations.ObjectInternals;
import org.openjdk.jol.operations.StringCompress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String... args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: jol-cli.jar <mode> [optional arguments]*");
            System.err.println("  Use mode \"help\" to get the list of available modes.");
            System.exit(1);
        }

        String mode = args[0];

        List<Operation> operations = new ArrayList<Operation>();
        operations.add(new ObjectInternals());
        operations.add(new ObjectExternals());
        operations.add(new ObjectEstimates());
        operations.add(new ObjectFootprint());
        operations.add(new ObjectIdealPacking());
        operations.add(new StringCompress());
        operations.add(new HeapDump());

        if (mode.equals("help")) {
            System.out.println("Available modes: ");
            for (Operation op : operations) {
                System.out.printf("  %20s: %s%n", op.label(), op.description());
            }
            System.exit(0);
        }

        String[] modeArgs = Arrays.copyOfRange(args, 1, args.length);
        for (Operation op : operations) {
            if (op.label().equals(mode)) {
                op.run(modeArgs);
            }
        }
    }

}
