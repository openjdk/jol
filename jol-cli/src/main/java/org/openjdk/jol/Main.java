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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

public class Main {

    private static SortedMap<String, Operation> operations = new TreeMap<String, Operation>();

    static {
        registerOperation(new ObjectInternals());
        registerOperation(new ObjectExternals());
        registerOperation(new ObjectEstimates());
        registerOperation(new ObjectFootprint());
        registerOperation(new ObjectIdealPacking());
        registerOperation(new StringCompress());
        registerOperation(new HeapDump());
    }

    private static void registerOperation(Operation op) {
        operations.put(op.label(), op);
    }

    public static void main(String... args) throws Exception {
        String mode = (args.length >= 1) ? args[0] : "help";

        Operation op = operations.get(mode);
        if (op != null) {
            String[] modeArgs = Arrays.copyOfRange(args, 1, args.length);
            op.run(modeArgs);
        } else {
            if (!mode.equals("help")) {
                System.err.println("Unknown mode: " + mode);
                System.err.println();
                printHelp(System.err);
                System.exit(1);
            } else {
                printHelp(System.out);
                System.exit(0);
            }
        }
    }

    private static void printHelp(PrintStream pw) {
        pw.println("Usage: jol-cli.jar <mode> [optional arguments]*");
        pw.println();

        pw.println("Available modes: ");
        for (Operation lop : operations.values()) {
            pw.printf("  %20s: %s%n", lop.label(), lop.description());
        }
    }

}
