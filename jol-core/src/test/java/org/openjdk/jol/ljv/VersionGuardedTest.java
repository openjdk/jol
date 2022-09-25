/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jol.ljv;

public class VersionGuardedTest {
    int VERSION = getVersion();

    public boolean is11() {
        return VERSION == 11;
    }

    static int getVersion() {
//        Java 8 or lower: 1.6.0_23, 1.7.0, 1.7.0_80, 1.8.0_211
//        Java 9 or higher: 9.0.1, 11.0.4, 12, 12.0.1
        String version = "-1";
        String fullVersion = System.getProperty("java.version");
        if (fullVersion.startsWith("1.")) {
            version = fullVersion.substring(2, 3);
        } else {
            int dotPos = fullVersion.indexOf(".");
            if (dotPos != -1) {
                version = fullVersion.substring(0, dotPos);
            }
        }
        return Integer.parseInt(version);
    }
}
