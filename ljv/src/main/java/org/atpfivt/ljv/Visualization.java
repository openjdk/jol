package org.atpfivt.ljv;


public interface Visualization {
    void beginDOT();
    String finishDOT();

    boolean alreadyVisualized(Object obj);

    void visitNull();

    void visitArrayBegin(Object array, boolean hasPrimitiveValues);
    void visitArrayElement(Object array, Object element, int elementIndex, boolean isPrimitive);
    void visitArrayElementObjectConnection(Object array, int elementIndex, Object obj);
    void visitArrayEnd(Object array);

    void visitObjectBegin(Object obj, String className, int primitiveFieldsNum);
    void visitObjectPrimitiveField(Object obj, String fieldName, String fieldValueStr);
    void visitObjectEnd(Object obj);
    void visitObjectFieldRelationWithNonPrimitiveObject(Object obj, String fieldName, String ljvFieldAttributes, Object relatedObject);
}
