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

import org.openjdk.jol.vm.sa.ServiceabilityAgentSupport;
import org.openjdk.jol.vm.sa.UniverseData;
import sun.misc.Unsafe;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class VM {

    private static Unsafe tryUnsafe() {
        return AccessController.doPrivileged(
                new PrivilegedAction<Unsafe>() {
                    @Override
                    public Unsafe run() {
                        try {
                            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
                            unsafe.setAccessible(true);
                            return (Unsafe) unsafe.get(null);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
        );
    }

    private static VirtualMachine INSTANCE;

    public static VirtualMachine current() {
        if (INSTANCE != null) return INSTANCE;

        String name = System.getProperty("java.vm.name");
        if (!name.contains("HotSpot") && !name.contains("OpenJDK")) {
            throw new IllegalStateException("Only HotSpot/OpenJDK VMs are supported");
        }

        Unsafe u = tryUnsafe();
        if (u == null) {
            throw new IllegalStateException("Unsafe is not available.");
        }

        Instrumentation inst = null;
        try {
            inst = InstrumentationSupport.instance();
        } catch (Exception e) {
            System.out.println("# WARNING: Unable to get Instrumentation. " + e.getMessage());
        }

        try {
            UniverseData saDetails = ServiceabilityAgentSupport.instance().getUniverseData();
            INSTANCE = new HotspotUnsafe(u, inst, saDetails);
        } catch (Exception e) {
            System.out.println("# WARNING: Unable to attach Serviceability Agent. " + e.getMessage());
            INSTANCE = new HotspotUnsafe(u, inst);
        }

        return INSTANCE;
    }

}
