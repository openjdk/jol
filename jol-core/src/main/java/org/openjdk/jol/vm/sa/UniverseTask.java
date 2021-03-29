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
package org.openjdk.jol.vm.sa;

import org.openjdk.jol.util.ClassUtils;

import java.lang.reflect.Method;

import static org.openjdk.jol.vm.sa.Constants.*;

/**
 * {@link Task} implementation to find compressed reference informations.
 *
 * @author Serkan Ozal
 */
@SuppressWarnings("serial")
class UniverseTask implements Task {

    @Override
    public UniverseData process() {
        try {
            Class<?> universeClass = ClassUtils.loadClass(UNIVERSE_CLASSNAME);
            Class<?> vmClass = ClassUtils.loadClass(VM_CLASSNAME);
            Object vm = ClassUtils.loadClass(VM_CLASSNAME).getMethod("getVM").invoke(null);

            Method getOopSizeMethod = vmClass.getMethod("getOopSize");
            Method getObjectAlignmentInBytesMethod = vmClass.getMethod("getObjectAlignmentInBytes");

            Method getHeapOopSizeMethod = vmClass.getMethod("getHeapOopSize");
            Method isCompressedOopsEnabledMethod = vmClass.getMethod("isCompressedOopsEnabled");
            Method getNarrowOopBaseMethod = universeClass.getMethod("getNarrowOopBase");
            Method getNarrowOopShiftMethod = universeClass.getMethod("getNarrowOopShift");

            Method isCompressedKlassPtrsEnabledMethod = null;
            Method getNarrowKlassBaseMethod = null;
            Method getNarrowKlassShiftMethod = null;

            try {
                isCompressedKlassPtrsEnabledMethod = vmClass.getMethod("isCompressedKlassPointersEnabled");
                getNarrowKlassBaseMethod = universeClass.getMethod("getNarrowKlassBase");
                getNarrowKlassShiftMethod = universeClass.getMethod("getNarrowKlassShift");
            } catch (NoSuchMethodException e) {
                // There is nothing to do, seems target JVM is not Java 8
            }

            int addressSize = ((Long) getOopSizeMethod.invoke(vm)).intValue();
            int objectAlignment = (Integer) getObjectAlignmentInBytesMethod.invoke(vm);

            int oopSize = (Integer) getHeapOopSizeMethod.invoke(vm);
            boolean compressedOopsEnabled = (Boolean) isCompressedOopsEnabledMethod.invoke(vm);
            long narrowOopBase = (Long) getNarrowOopBaseMethod.invoke(null);
            int narrowOopShift = (Integer) getNarrowOopShiftMethod.invoke(null);

            /*
             * If compressed klass references is not supported (before Java 8),
             * use compressed oop references values instead of them.
             */

            boolean compressedKlassPtrsEnabled = isCompressedKlassPtrsEnabledMethod != null ?
                    (Boolean) isCompressedKlassPtrsEnabledMethod.invoke(vm) : compressedOopsEnabled;
            long narrowKlassBase = getNarrowKlassBaseMethod != null ?
                    (Long) getNarrowKlassBaseMethod.invoke(null) : narrowOopBase;
            int narrowKlassShift = getNarrowKlassShiftMethod != null ?
                    (Integer) getNarrowKlassShiftMethod.invoke(null) : narrowOopShift;

            return new UniverseData(addressSize,
                                                        objectAlignment,
                                                        oopSize,
                                                        compressedOopsEnabled,
                                                        narrowOopBase,
                                                        narrowOopShift,
                                                        compressedKlassPtrsEnabled,
                                                        narrowKlassBase,
                                                        narrowKlassShift);
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage(), t);
        }
    }

}
