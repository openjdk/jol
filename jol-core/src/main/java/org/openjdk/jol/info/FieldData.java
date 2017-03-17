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
package org.openjdk.jol.info;

import org.openjdk.jol.util.ClassUtils;
import org.openjdk.jol.util.ObjectUtils;
import org.openjdk.jol.vm.ContendedSupport;
import org.openjdk.jol.vm.VM;

import java.lang.reflect.Field;

/**
 * Holds the field information, without the layout.
 *
 * @author Aleksey Shipilev
 */
public class FieldData {

    /**
     * Creates the synthetic field data.
     *
     * @param hostKlass class name where the field is declared
     * @param fieldName field name
     * @param fieldType string representation of field type
     * @return field data
     */
    public static FieldData create(String hostKlass, String fieldName, String fieldType) {
        return new FieldData(null, -1, hostKlass, fieldName, fieldType, false, null);
    }

    /**
     * Parses the field data from the existing field.
     *
     * @param field field to parse
     * @return field data
     */
    public static FieldData parse(Field field) {
        return new FieldData(
                field,
                VM.current().fieldOffset(field),
                ClassUtils.getSafeName(field.getDeclaringClass()),
                field.getName(),
                ClassUtils.getSafeName(field.getType()),
                ContendedSupport.isContended(field),
                ContendedSupport.contendedGroup(field)
        );
    }

    private final String name;
    private final String type;
    private final String klass;
    private final Field refField;
    private final long vmOffset;
    private final boolean isContended;
    private final String contendedGroup;

    private FieldData(Field refField, long vmOffset, String hostKlass, String fieldName, String fieldType,
                      boolean isContended, String contendedGroup) {
        this.klass = hostKlass;
        this.name = fieldName;
        this.type = fieldType;
        this.refField = refField;
        this.vmOffset = vmOffset;
        this.isContended = isContended;
        this.contendedGroup = contendedGroup;
    }

    /**
     * Answers the class for the field type
     *
     * @return string representation of field type
     */
    public String typeClass() {
        return type;
    }

    /**
     * Answers the class for the field holder.
     *
     * @return string representation of holder class
     */
    public String hostClass() {
        return klass;
    }

    /**
     * Answers the field name.
     *
     * @return field name
     */
    public String name() {
        return name;
    }

    /**
     * Answers whether the field has contentded annotation.
     *
     * @return true, if the field is contended
     */
    public boolean isContended() {
        return isContended;
    }

    /**
     * Get contentded group of the field.
     *
     * @return String
     */
    public String contendedGroup() {
        return contendedGroup;
    }

    /**
     * Get original Field.
     *
     * @return Field which is represented by the FieldData
     */
    public Field refField() {
        return refField;
    }

    /**
     * The VM offset for the field, as discovered.
     * Some layouters need to know this.
     *
     * @return vm offset
     */
    public long vmOffset() {
        if (vmOffset != -1) {
            return vmOffset;
        } else {
            throw new IllegalStateException("VM offset is not defined");
        }
    }

    public String toString() {
        return name + ": " + type;
    }
}
