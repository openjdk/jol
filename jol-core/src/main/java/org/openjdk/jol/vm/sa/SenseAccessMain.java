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
package org.openjdk.jol.vm.sa;

import org.openjdk.jol.util.ClassUtils;

import static org.openjdk.jol.vm.sa.Constants.*;

class SenseAccessMain {

    public static void main(final String[] args) {
        try {
            System.setProperty("sun.jvm.hotspot.debugger.useProcDebugger", "true");
            System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");

            ClassUtils.loadClass(VM_CLASSNAME).getMethod("getVM");
            ClassUtils.loadClass(HOTSPOT_AGENT_CLASSNAME).newInstance();
            ClassUtils.loadClass(HOTSPOT_AGENT_CLASSNAME).getMethod("attach",int.class);
            ClassUtils.loadClass(HOTSPOT_AGENT_CLASSNAME).getMethod("detach");
        } catch (Throwable t) {
            System.exit(1);
        }
    }
}
