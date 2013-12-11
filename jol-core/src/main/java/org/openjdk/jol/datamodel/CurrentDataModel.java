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
package org.openjdk.jol.datamodel;

import org.openjdk.jol.util.VMSupport;

/**
 * Current data model as detected by JVM.
 *
 * @author Aleksey Shipilev
 */
public class CurrentDataModel implements DataModel {
    @Override
    public int headerSize() {
        return VMSupport.OBJ_HEADER_SIZE;
    }

    @Override
    public int sizeOf(String klass) {
        if (klass.equals("byte"))    return VMSupport.BYTE_SIZE;
        if (klass.equals("boolean")) return VMSupport.BOOLEAN_SIZE;
        if (klass.equals("short"))   return VMSupport.SHORT_SIZE;
        if (klass.equals("char"))    return VMSupport.CHAR_SIZE;
        if (klass.equals("int"))     return VMSupport.INT_SIZE;
        if (klass.equals("float"))   return VMSupport.FLOAT_SIZE;
        if (klass.equals("long"))    return VMSupport.LONG_SIZE;
        if (klass.equals("double"))  return VMSupport.DOUBLE_SIZE;
        return VMSupport.REF_SIZE;
    }
}
