package org.atpfivt.ljv;

import java.util.IdentityHashMap;

public class VisualizationCommon implements Visualization {
    private final StringBuilder out = new StringBuilder();
    private final LJV ljv;
    private final IdentityHashMap<Object, String> alreadyDrawnObjectsIds = new IdentityHashMap<>();
    private boolean alreadyDrawnNull = false;

    public VisualizationCommon(LJV ljv) {
        this.ljv = ljv;
    }

    @Override
    public boolean alreadyVisualized(Object obj) {
        if (obj == null) {
            return alreadyDrawnNull;
        }

        return alreadyDrawnObjectsIds.containsKey(obj);
    }

    @Override
    public void beginDOT() {
        out.setLength(0); // Clearing String Builder before starting new DOT
        out.append("digraph Java {\n")
                .append("\trankdir=\"")
                .append(ljv.getDirection())
                .append("\";\n")
                .append("\tnode[shape=plaintext]\n");
    }

    @Override
    public String finishDOT() {
        out.append("}\n");
        return out.toString();
    }

    @Override
    public void visitNull() {
        if (!alreadyDrawnNull) {
            out.append("\t").append(dotName(null)).append("[label=\"null\"").append(", shape=plaintext];\n");
            alreadyDrawnNull = true;
        }
    }

    @Override
    public void visitArrayBegin(Object array, boolean hasPrimitiveValues) {
        out.append("\t")
           .append(dotName(array))
           .append("[label=<\n");

        if (hasPrimitiveValues) {
           out.append("\t\t<table border='0' cellborder='1' cellspacing='0'>\n");
        } else {
           out.append("\t\t<table border='0' cellborder='1' cellspacing='0' cellpadding='9'>\n");
        }

        out.append("\t\t\t<tr>\n");
    }

    @Override
    public void visitArrayElement(Object array, Object element, int elementIndex, boolean isPrimitive) {        
        out.append("\t\t\t\t<td");
        if (!isPrimitive) {
            out.append(" port=\"f").append(elementIndex).append("\"");
        }
        out.append(ljv.getArrayElementAttributes(array, elementIndex))
           .append(">");

        // If array element is treated as primitive - than filling array cell with value
        // Otherwise cell will be empty, but arrow-connected with object it is containing
        if (isPrimitive) {
            out.append(Quote.quote(String.valueOf(element)));
        }

        out.append("</td>\n");
    }

    @Override
    public void visitArrayElementObjectConnection(Object array, int elementIndex, Object obj) {
        out.append("\t")
           .append(dotName(array))
           .append(":f")
           .append(elementIndex)
           .append(" -> ")
           .append(dotName(obj))
           .append("[label=\"")
           .append(elementIndex)
           .append("\",fontsize=12];\n");
    }


    @Override
    public void visitArrayEnd(Object array) {
        out.append("\t\t\t</tr>\n\t\t</table>\n\t>];\n");
    }

    @Override
    public void visitObjectBegin(Object obj, String className, int primitiveFieldsNum) {
        out.append("\t")
           .append(dotName(obj))
           .append("[label=<\n")
           .append("\t\t<table border='0' cellborder='1' cellspacing='0'>\n");

        // Adding header row with object class name
        out.append("\t\t\t<tr>\n");
        if (primitiveFieldsNum > 0) {
            out.append("\t\t\t\t<td rowspan='")
               .append(primitiveFieldsNum + 1)
               .append("'>");
        } else {
            out.append("\t\t\t\t<td>");
        }
        out.append(className)
           .append("</td>\n\t\t\t</tr>\n");
    }
    
    @Override
    public void visitObjectPrimitiveField(Object obj, String fieldName, String fieldValueStr) {
        out.append("\t\t\t<tr>\n\t\t\t\t<td>");
        if (ljv.isShowFieldNamesInLabels()) {
            out.append(fieldName).append(": ");
        }
        out.append(Quote.quote(fieldValueStr));
        out.append("</td>\n\t\t\t</tr>\n");
    }

    @Override
    public void visitObjectFieldRelationWithNonPrimitiveObject(Object obj, String fieldName, String ljvFieldAttributes, Object relatedObject) {
        out.append("\t")
           .append(dotName(obj))
           .append(" -> ")
           .append(dotName(relatedObject))
           .append("[label=\"")
           .append(fieldName)
           .append("\",fontsize=12")
           .append(ljvFieldAttributes.isEmpty() ? "" : "," + ljvFieldAttributes)
           .append("];\n");
    }

    @Override
    public void visitObjectEnd(Object obj) {
        out.append("\t\t</table>\n\t>");

        String cabs = ljv.getObjectAttributes(obj);
        if (!cabs.isEmpty()) {
            out.append(",").append(cabs);
        }
        out.append("];\n");
    }

    private String dotName(Object obj) {
        return obj == null ? "NULL" : alreadyDrawnObjectsIds.computeIfAbsent(obj, s -> "n" + (alreadyDrawnObjectsIds.size() + 1));
    }
}
