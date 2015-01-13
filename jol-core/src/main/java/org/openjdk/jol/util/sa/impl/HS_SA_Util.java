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
package org.openjdk.jol.util.sa.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openjdk.jol.util.HS_SA_Support;

/**
 * Hotspot Serviceability Agent utility. It was designed for doing some utility stuff without touching
 * {@link HS_SA_Support} class because of its static initializer.
 *
 * @author Serkan Ozal
 */
public class HS_SA_Util {

    public static final String HOTSPOT_AGENT_CLASSNAME = "sun.jvm.hotspot.HotSpotAgent";
    public static final String VM_CLASSNAME = "sun.jvm.hotspot.runtime.VM";
    public static final String UNIVERSE_CLASSNAME = "sun.jvm.hotspot.memory.Universe";

    private HS_SA_Util() {

    }

    public static Class<?> getHotspotAgentClass() throws ClassNotFoundException {
        return Class.forName(HOTSPOT_AGENT_CLASSNAME);
    }

    public static Object createHotspotAgentInstance()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> hotspotAgentClass = Class.forName(HOTSPOT_AGENT_CLASSNAME);
        return hotspotAgentClass.newInstance();
    }

    public static Class<?> getVmClass() throws ClassNotFoundException {
        return Class.forName(VM_CLASSNAME);
    }

    public static Object getVMInstance()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException,
                   NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        Class<?> vmClass = Class.forName(VM_CLASSNAME);
        Method getVmMethod = vmClass.getMethod("getVM");
        return getVmMethod.invoke(null);
    }

    public static Class<?> getUniverseClass() throws ClassNotFoundException {
        return Class.forName(UNIVERSE_CLASSNAME);
    }

}
