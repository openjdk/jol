package org.atpfivt.ljv;

import org.atpfivt.ljv.jol.ClassLayout;
import org.atpfivt.ljv.jol.FieldLayout;
import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.util.ObjectUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Array;

import java.util.SortedSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IntrospectionWithReflectionAPI extends IntrospectionBase {
    public IntrospectionWithReflectionAPI(LJV ljv) {
        super(ljv);
    }

    @Override
    public Field[] getObjFields(Object obj) {
        Class<?> cls = obj.getClass();

        SortedSet<FieldLayout> fieldLayouts = ClassLayout.parseClass(cls).fields();

        Stream<Field> fieldStream = fieldLayouts.stream()
                .map(FieldLayout::data).map(FieldData::refField).filter(getObjFieldsIgnoreNullValuedPredicate(obj));

        return fieldStream.toArray(Field[]::new);
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
    public boolean catTreatObjAsArrayOfPrimitives(Object obj) {
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

    private Predicate<Field> getObjFieldsIgnoreNullValuedPredicate(Object obj) {
        return (Field f) -> {
            if (ljv.isIgnoreNullValuedFields()) {
                return ObjectUtils.value(obj, f) != null;
            }
            return true;
        };
    }
}
