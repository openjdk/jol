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
package org.openjdk.jol.vm;

import javax.management.*;
import javax.management.openmbean.CompositeDataSupport;
import java.lang.management.ManagementFactory;

/**
 * Polls VM options.
 */
class VMOptions {

    private static String getString(String key) throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName mbean = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
        CompositeDataSupport val = (CompositeDataSupport) server.invoke(mbean, "getVMOption", new Object[]{key}, new String[]{"java.lang.String"});
        return val.get("value").toString();
    }

    public static Boolean pollCompressedOops() {
        try {
            return Boolean.valueOf(getString("UseCompressedOops"));
        } catch (Exception exp) {
            return null;
        }
    }

    public static Boolean pollCompressedClassPointers() {
        try {
            return Boolean.valueOf(getString("UseCompressedClassPointers"));
        } catch (Exception exp) {
            return null;
        }
    }

    public static Integer pollObjectAlignment() {
        if (Boolean.TRUE.equals(pollCompressedOops())) {
            try {
                return Integer.valueOf(getString("ObjectAlignmentInBytes"));
            } catch (Exception exp) {
                return null;
            }
        }
        return null;
    }

}
