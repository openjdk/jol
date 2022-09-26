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

import org.openjdk.jol.ljv.nodes.*;
import org.openjdk.jol.info.FieldLayout;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.util.ObjectUtils;

import java.lang.reflect.*;
import java.util.*;

public class IntrospectionWithReflectionAPI implements Introspection {
    private final IdentityHashMap<Object, ObjectNode> alreadyVisitedObjects = new IdentityHashMap<>();
    protected final LJV ljv;

    public IntrospectionWithReflectionAPI(LJV ljv) {
        this.ljv = ljv;
    }

    @Override
    public Node parseGraph(Object obj, String name, boolean isPrimitive, Field field) {
        if (isPrimitive) {
            return new PrimitiveNode(obj, name);
        }

        if (obj == null) {
            return new NullNode(null, name);
        }
        
        ObjectNode oldNode = alreadyVisitedObjects.get(obj);
        if (oldNode != null) {
            ObjectNode objectNode = new ObjectNode(oldNode);
            objectNode.setName(name);
            if (field != null) {
                objectNode.setAttributes(ljv.getFieldAttributes(field, obj));
            }
            return objectNode;
        }

        if (obj.getClass().isArray()) {
            ArrayNode arrayNode = new ArrayNode(obj, name, canTreatObjAsArrayOfPrimitives(obj), getArrayContent(obj));
            if (field != null) {
                arrayNode.setAttributes(ljv.getFieldAttributes(field, obj));
            }
            return arrayNode;
        }

        ObjectNode objectNode = new ObjectNode(obj, name,
                getObjClassName(obj, false),
                countObjectPrimitiveFields(obj), null,
                "");
        alreadyVisitedObjects.put(obj, objectNode);
        if (field != null) {
            objectNode.setAttributes(ljv.getFieldAttributes(field, obj));
        }
        objectNode.setChildren(getChildren(obj));
        return objectNode;
    }

    @Override
    public List<Node> getChildren(Object obj) {
        List<Node> result = new ArrayList<>();

        Field[] fields = getObjFields(obj);

        for (Field field : fields) {
            if (!(Modifier.isStatic(field.getModifiers()))
                    && !ljv.canIgnoreField(field)
            ) {
                Node node = parseGraph(ObjectUtils.value(obj, field), field.getName(), objectFieldIsPrimitive(field, obj), field);
                if (node != null) result.add(node);
            }
        }
        return result;
    }

    private List<Node> getArrayContent(Object obj) {
        List<Node> result = new ArrayList<>();
        int len = Array.getLength(obj);
        for (int i = 0; i < len; i++) {
            Object ref = Array.get(obj, i);
            result.add(parseGraph(ref, getObjClassName(ref, false), false, null));
        }
        return result;
    }

    @Override
    public boolean canBeConvertedToString(Object obj) {
        Method[] ms = obj.getClass().getMethods();
        for (Method m : ms) {
            if (m.getName().equals("toString") && m.getDeclaringClass() != Object.class) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getObjClassName(Object obj, boolean useToStringAsClassName) {
        if (obj == null)
            return "";

        Class<?> c = obj.getClass();
        if (useToStringAsClassName && canBeConvertedToString(obj))
            return Quote.quote(obj.toString());
        else {
            String name = c.getName();
            if (!ljv.isShowPackageNamesInClasses() || c.getPackage() == ljv.getClass().getPackage()) {
                //- Strip away everything before the last .
                name = name.substring(name.lastIndexOf('.') + 1);

                if (!ljv.isQualifyNestedClassNames())
                    name = name.substring(name.lastIndexOf('$') + 1);
            }
            return name;
        }
    }

    @Override
    public int countObjectPrimitiveFields(Object obj) {
        int size = 0;
        Field[] fields = getObjFields(obj);
        for (Field field : fields) {
            if (objectFieldIsPrimitive(field, obj)) {
                size++;
            }
        }
        return size;
    }

    @Override
    public boolean hasPrimitiveFields(Object obj) {
        return countObjectPrimitiveFields(obj) > 0;
    }

    @Override
    public boolean objectFieldIsPrimitive(Field field, Object obj) {
        if (!ljv.canIgnoreField(field)) {
            //- The order of these statements matters. It is not correct
            //- to return true if field.getType( ).isPrimitive( )
            Object val = ObjectUtils.value(obj, field);
            //- Just calling ljv.canTreatAsPrimitive is not adequate --
            //- val will be wrapped as a Boolean or Character, etc. if we
            //- are dealing with a truly primitive type.
            return field.getType().isPrimitive() || canTreatObjAsPrimitive(val);
        }

        return false;
    }

    @Override
    public Field[] getObjFields(Object obj) {
        Class<?> cls = obj.getClass();

        SortedSet<FieldLayout> fieldLayouts = ClassLayout.parseClass(cls).fields();
        ArrayList<Field> res = new ArrayList<>();
        for (FieldLayout layout : fieldLayouts) {
            Field f = layout.data().refField();
            if (ljv.isIgnoreNullValuedFields()) {
                if (ObjectUtils.value(obj, f) == null) {
                    continue;
                }
            }
            res.add(f);
        }
        return res.toArray(new Field[0]);
    }


    @Override
    public boolean canTreatClassAsPrimitive(Class<?> cz) {
        if (cz == null || cz.isPrimitive())
            return true;

        if (cz.isArray())
            return false;

        do {
            if (ljv.isTreatsAsPrimitive(cz)
                    || ljv.isTreatsAsPrimitive(cz.getPackage())
            )
                return true;

            if (cz == Object.class)
                return false;

            Class<?>[] ifs = cz.getInterfaces();
            for (Class<?> anIf : ifs)
                if (canTreatClassAsPrimitive(anIf))
                    return true;

            cz = cz.getSuperclass();
        } while (cz != null);
        return false;
    }

    @Override
    public boolean canTreatObjAsPrimitive(Object obj) {
        return obj == null || canTreatClassAsPrimitive(obj.getClass());
    }

    @Override
    public boolean canTreatObjAsArrayOfPrimitives(Object obj) {
        Class<?> c = obj.getClass();
        if (c.getComponentType().isPrimitive()) {
            return true;
        }

        for (int i = 0, len = Array.getLength(obj); i < len; i++) {
            if (!canTreatObjAsPrimitive(Array.get(obj, i))) {
                return false;
            }
        }

        return true;
    }

}
