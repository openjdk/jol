package org.atpfivt.ljv;

import org.reflections.ReflectionUtils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Array;

import java.util.function.Predicate;

public class IntrospectionWithReflectionAPI extends IntrospectionBase {
    public IntrospectionWithReflectionAPI(LJV ljv) {
        super(ljv);
    }

    @Override
    public Field[] getObjFields(Object obj) {
        Class<?> cls = obj.getClass();

        Field[] fs = ReflectionUtils.getAllFields(cls, getObjFieldsIgnoreNullValuedPredicate(obj))
                                    .toArray(new Field[0]);
        normalizeFieldsOrder(fs);
        if (!ljv.isIgnorePrivateFields()) {
            AccessibleObject.setAccessible(fs, true);
        }

        return fs;
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
            try {
                //- The order of these statements matters.  If field is not
                //- accessible, we want an IllegalAccessException to be raised
                //- (and caught).  It is not correct to return true if
                //- field.getType( ).isPrimitive( )
                Object val = field.get(obj);
                if (field.getType().isPrimitive() || canTreatObjAsPrimitive(val))
                    //- Just calling ljv.canTreatAsPrimitive is not adequate --
                    //- val will be wrapped as a Boolean or Character, etc. if we
                    //- are dealing with a truly primitive type.
                    return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
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
                try {
                    f.setAccessible(true);
                    return f.get(obj) != null;
                } catch (IllegalAccessException e) {
                    return false;
                }
            }
            return true;
        };
    }

    private static void normalizeFieldsOrder(Field[] fs) {
        /*Ensure that 'left' field is always processed before 'right'.
        The problem is that ReflectionUtils.getAllFields uses HashSet, not LinkedHashSet,
        and loses information about fields order.

        This is a hard-coded logic and should be removed in the future.
         */
        int i = 0, left = -1, right = -1;
        for (Field f : fs) {
            if ("left".equals(f.getName())) {
                left = i;
                break;
            } else if ("right".equals(f.getName())) {
                right = i;
            }
            i++;
        }
        if (right > -1 && left > right) {
            //swap left & right
            Field f = fs[left];
            fs[left] = fs[right];
            fs[right] = f;
        }
    }
}
