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

            Method vmMethod = vmClass.getMethod("getVM");

            Method heapOopSizeMethod = vmClass.getMethod("getHeapOopSize");
            Method oopSizeMethod = vmClass.getMethod("getOopSize");
            Method objectAlignmentMethod = vmClass.getMethod("getObjectAlignmentInBytes");

            Method compOopsEnabledMethod = vmClass.getMethod("isCompressedOopsEnabled");
            Method compKlassEnabledMethod = vmClass.getMethod("isCompressedKlassPointersEnabled");

            Method narrowOopBaseMethod = null;
            Method narrowOopShiftMethod = null;
            try {
                // Past JDK 13, JDK-8223136, we have a special class for this data.
                Class<?> coopClass = ClassUtils.loadClass(COMP_OOPS_CLASSNAME);
                narrowOopBaseMethod = coopClass.getMethod("getBase");
                narrowOopShiftMethod = coopClass.getMethod("getShift");
            } catch (Exception e) {
                narrowOopBaseMethod = universeClass.getMethod("getNarrowOopBase");
                narrowOopShiftMethod = universeClass.getMethod("getNarrowOopShift");
            }

            Method narrowKlassBaseMethod = null;
            Method narrowKlassShiftMethod = null;
            try {
                // Past JDK 13, JDK-8223136, we have a special class for this data.
                Class<?> coopClass = ClassUtils.loadClass(COMP_KLASS_CLASSNAME);
                narrowKlassBaseMethod = coopClass.getMethod("getBase");
                narrowKlassShiftMethod = coopClass.getMethod("getShift");
            } catch (Exception e) {
                narrowKlassBaseMethod = universeClass.getMethod("getNarrowKlassBase");
                narrowKlassShiftMethod = universeClass.getMethod("getNarrowKlassShift");
            }

            Object vm = vmMethod.invoke(null);

            return new UniverseData(
                    ((Long) oopSizeMethod.invoke(vm)).intValue(),
                    (Integer) objectAlignmentMethod.invoke(vm),
                    (Integer) heapOopSizeMethod.invoke(vm),
                    (Boolean) compOopsEnabledMethod.invoke(vm),
                    (Long) narrowOopBaseMethod.invoke(null),
                    (Integer) narrowOopShiftMethod.invoke(null),
                    (Boolean) compKlassEnabledMethod.invoke(vm),
                    (Long) narrowKlassBaseMethod.invoke(null),
                    (Integer) narrowKlassShiftMethod.invoke(null)
            );
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage(), t);
        }
    }

}
