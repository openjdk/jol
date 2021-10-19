package org.atpfivt.ljv;

import java.lang.reflect.Field;

public interface Introspection {

    public String getObjClassName(Object obj, boolean useToStringAsClassName);

    public Field[] getObjFields(Object obj);

    public int countObjectPrimitiveFields(Object obj);

    public boolean hasPrimitiveFields(Object obj);

    public boolean objectFieldIsPrimitive(Field field, Object obj);

    public boolean canBeConvertedToString(Object obj);

    public boolean canTreatObjAsPrimitive(Object obj);

    public boolean catTreatObjAsArrayOfPrimitives(Object obj);

    public boolean canTreatClassAsPrimitive(Class<?> cz);
}
