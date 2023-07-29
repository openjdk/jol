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
package org.openjdk.jol.util;

import java.util.ArrayList;

public class ArrayDimensionParser {

    public static int[] parse(final String dimensionsString) {
        final ArrayList<String> dimensionValues = new ArrayList<>();
        State state = State.INIT;
        StringBuilder sb = null;
        for (int i = 0; i < dimensionsString.length(); i++) {
            char currentChar = dimensionsString.charAt(i);
            switch (state) {
                case INIT: {
                    if (currentChar == '[') {
                        state = State.DIM_START;
                    } else {
                        throw new IllegalStateException();
                    }
                    break;
                }
                case DIM_START: {
                    if (Character.isDigit(currentChar)) {
                        state = State.DIM_VALUE;
                        sb = new StringBuilder();
                        sb.append(currentChar);
                    } else if (currentChar == ']') {
                        state = State.DIM_END;
                    } else {
                        throw new IllegalStateException();
                    }
                    break;
                }
                case DIM_VALUE: {
                    if (currentChar == ']') {
                        state = State.DIM_END;
                    } else if (Character.isDigit(currentChar)) {
                        sb.append(currentChar);
                    } else {
                        throw new IllegalStateException();
                    }
                    break;
                }
                case DIM_END: {
                    if (currentChar == '[') {
                        state = State.DIM_START;
                        dimensionValues.add(getDimensionValue(sb));
                        sb = null;
                    } else {
                        throw new IllegalStateException();
                    }
                    break;
                }
            }
        }

        if (state == State.DIM_END) {
            dimensionValues.add(getDimensionValue(sb));
        } else {
            throw new IllegalStateException();
        }

        final int[] result = new int[dimensionValues.size()];
        for (int i = 0; i < dimensionValues.size(); i++) {
            result[i] = Integer.parseInt(dimensionValues.get(i));
        }

        return result;
    }

    private static String getDimensionValue(StringBuilder sb) {
        final String dimensionValue = sb == null ? "" : sb.toString();
        if (dimensionValue.isEmpty()) {
            return "0";
        } else {
            return dimensionValue;
        }
    }

    private enum State {
        INIT, DIM_START, DIM_VALUE, DIM_END
    }
}
