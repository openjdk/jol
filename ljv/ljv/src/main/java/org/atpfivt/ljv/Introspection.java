package org.atpfivt.ljv;

import org.atpfivt.ljv.nodes.Node;

import java.lang.reflect.Field;
import java.util.List;

public interface Introspection {

    Node parseGraph(Object obj, String name, boolean isPrimitive, Field field);

    List<Node> getChildren(Object obj);

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