package org.atpfivt.ljv;

import org.openjdk.jol.util.ObjectUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

final class GraphBuilder {
    private final LJV ljv;
    private final Introspection introspection;
    private Visualization visualization;

    public GraphBuilder(LJV ljv) {
        this.ljv = ljv;
        this.introspection = new IntrospectionWithReflectionAPI(ljv);
        this.visualization = new VisualizationCommon(ljv);
    }

    public String generateDOT() {
        visualization.beginDOT();
        for (Object obj : ljv.getRoots()) {
            processNode(obj);
        }
        return visualization.finishDOT();
    }

    private void processArray(Object obj) {
        boolean valuesArePrimitive = introspection.catTreatObjAsArrayOfPrimitives(obj);
        int len = Array.getLength(obj);

        visualization.visitArrayBegin(obj, valuesArePrimitive);
        for (int i = 0; i < len; i++) {
            Object element = Array.get(obj, i);
            visualization.visitArrayElement(obj, element, i, valuesArePrimitive);
        }
        visualization.visitArrayEnd(obj);

        // Generating DOTs for array object elements and creating connection
        if (!valuesArePrimitive) {
            for (int i = 0; i < len; i++) {
                Object ref = Array.get(obj, i);
                if (ref == null)
                    continue;

                processNode(ref);
                visualization.visitArrayElementObjectConnection(obj, i, ref);
            }
        }
    }



    private void processObject(Object obj) {
        String className = introspection.getObjClassName(obj, false);
        Field[] fields = introspection.getObjFields(obj);
        int primitiveFieldsNum = introspection.countObjectPrimitiveFields(obj);

        visualization.visitObjectBegin(obj, className, primitiveFieldsNum);

        // First processing only primitive fields
        for (Field field : fields) {
            if (!introspection.objectFieldIsPrimitive(field, obj)) {
                continue;
            }

                Object ref = ObjectUtils.value(obj, field);
                if (introspection.objectFieldIsPrimitive(field, obj)) {
                    String name = field.getName();
                    String value = String.valueOf(ref);
                    visualization.visitObjectPrimitiveField(obj, name, value);
                }
        }
        visualization.visitObjectEnd(obj);

        // Next, processing non-primitive objects and making relations with them
        for (Field field : fields) {
            if (introspection.objectFieldIsPrimitive(field, obj) || ljv.canIgnoreField(field)) {
                continue;
            }

            Object ref = ObjectUtils.value(obj, field);;
            String name = field.getName();
            String fabs = ljv.getFieldAttributes(field, ref);

            processNode(ref);
            visualization.visitObjectFieldRelationWithNonPrimitiveObject(obj, name, fabs, ref);
        }
    }

    private void processNode(Object obj) {
        //TODO: кажется здесь должна быть логика "хотим ли мы скипать этот объект"
        //и получать NodeType
        //тогда этот if превращается в switch.
        if (visualization.alreadyVisualized(obj)) {
            //do nothing
        } else if (obj == null) {
            visualization.visitNull();
        } else if (obj.getClass().isArray()) {
            processArray(obj);
        } else {
            processObject(obj);
        }
    }
}
