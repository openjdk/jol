package org.atpfivt.ljv;

import java.lang.reflect.Field;

public interface Introspection {

    String getObjClassName(Object obj, boolean useToStringAsClassName);

    Field[] getObjFields(Object obj);

    int countObjectPrimitiveFields(Object obj);

    boolean hasPrimitiveFields(Object obj);

    boolean objectFieldIsPrimitive(Field field, Object obj);

    boolean canBeConvertedToString(Object obj);

    boolean canTreatObjAsPrimitive(Object obj);

    boolean catTreatObjAsArrayOfPrimitives(Object obj);

    boolean canTreatClassAsPrimitive(Class<?> cz);
}
