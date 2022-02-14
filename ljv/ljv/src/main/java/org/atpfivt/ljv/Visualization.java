package org.atpfivt.ljv;

import org.atpfivt.ljv.nodes.ArrayNode;
import org.atpfivt.ljv.nodes.Node;
import org.atpfivt.ljv.nodes.ObjectNode;

public interface Visualization {
    void diagramBegin();
    String diagramEnd();

    boolean alreadyVisualized(Object obj);

    void visitNull();

    void visitArrayBegin(ArrayNode arrayNode);
    void visitArrayElement(ArrayNode arrayNode, String element, int elementIndex);
    void visitArrayElementObjectConnection(Object array, int elementIndex, Object obj);
    void visitArrayEnd(Object array);

    void visitObjectBegin(ObjectNode objectNode);
    void visitObjectPrimitiveField(String fieldName, String fieldValueStr);
    void visitObjectEnd(Object obj);
    void visitObjectFieldRelationWithNonPrimitiveObject(Object obj, Node childNode, String ljvFieldAttributes);
}
