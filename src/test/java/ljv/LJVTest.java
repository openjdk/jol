package ljv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

public class LJVTest {

    private final LJV ljv = new LJV();


    @Test
    void stringIsNotAPrimitiveType() {
        String actual_graph_0 = ljv.drawGraph("Hello");

        String expected_graph_0 = "digraph Java {\n" +
                "n1[label=\"java.lang.String|{coder: 0|hash: 0}\",shape=record];\n" +
                "n2[shape=record, label=\"72|101|108|108|111\"];\n" +
                "n1 -> n2[label=\"value\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph_0, actual_graph_0, "Hello case failed");
    }

    @Test
    void objectArraysHoldReferencesPrimitiveArraysHoldValues() {
        String actual_graph_1 = ljv.drawGraph(
                new Context().setTreatAsPrimitive(String.class).setIgnorePrivateFields(false),
                new Object[]{new String[]{"a", "b", "c"}, new int[]{1, 2, 3}}
        );

        String expected_graph_1 = "digraph Java {\n" +
                "n1[label=\"<f0>|<f1>\",shape=record];\n" +
                "n2[shape=record, label=\"a|b|c\"];\n" +
                "n1:f0 -> n2[label=\"0\",fontsize=12];\n" +
                "n3[shape=record, label=\"1|2|3\"];\n" +
                "n1:f1 -> n3[label=\"1\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph_1, actual_graph_1, "Primitive array case failed");
    }

    @Test
    void assignmentDoesNotCreateANewObject() {
        String x = "Hello";
        String y = x;
        String actual_graph_2 = ljv.drawGraph(new Object[]{x, y});

        String expected_graph_2 = "digraph Java {\n" +
                "n1[label=\"<f0>|<f1>\",shape=record];\n" +
                "n2[label=\"java.lang.String|{coder: 0|hash: 0}\",shape=record];\n" +
                "n3[shape=record, label=\"72|101|108|108|111\"];\n" +
                "n2 -> n3[label=\"value\",fontsize=12];\n" +
                "n1:f0 -> n2[label=\"0\",fontsize=12];\n" +
                "n1:f1 -> n2[label=\"1\",fontsize=12];\n" +
                "}\n";


        assertEquals(expected_graph_2, actual_graph_2, "One link Hello case failed");
    }

    @Test
    void assignmentWithNewCreateANewObject() {
        String x = "Hello";
        String y = new String(x);
        String actual_graph_3 = ljv.drawGraph(new Object[]{x, y});

        String expected_graph_3 = "digraph Java {\n" +
                "n1[label=\"<f0>|<f1>\",shape=record];\n" +
                "n2[label=\"java.lang.String|{coder: 0|hash: 0}\",shape=record];\n" +
                "n3[shape=record, label=\"72|101|108|108|111\"];\n" +
                "n2 -> n3[label=\"value\",fontsize=12];\n" +
                "n1:f0 -> n2[label=\"0\",fontsize=12];\n" +
                "n4[label=\"java.lang.String|{coder: 0|hash: 0}\",shape=record];\n" +
                "n4 -> n3[label=\"value\",fontsize=12];\n" +
                "n1:f1 -> n4[label=\"1\",fontsize=12];\n" +
                "}\n";


        assertEquals(expected_graph_3, actual_graph_3, "Without duplicate hello case failed");
    }

    @Test
    void multiDimensionalArrays() {
        String actual_graph_4 = ljv.drawGraph(new int[4][5]);

        String expected_graph_4 = "digraph Java {\n" +
                "n1[label=\"<f0>|<f1>|<f2>|<f3>\",shape=record];\n" +
                "n2[shape=record, label=\"0|0|0|0|0\"];\n" +
                "n1:f0 -> n2[label=\"0\",fontsize=12];\n" +
                "n3[shape=record, label=\"0|0|0|0|0\"];\n" +
                "n1:f1 -> n3[label=\"1\",fontsize=12];\n" +
                "n4[shape=record, label=\"0|0|0|0|0\"];\n" +
                "n1:f2 -> n4[label=\"2\",fontsize=12];\n" +
                "n5[shape=record, label=\"0|0|0|0|0\"];\n" +
                "n1:f3 -> n5[label=\"3\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph_4, actual_graph_4, "Multiarray case failed");
    }

    @Test
    void cyclicalStructuresClassesWithAndWithoutAToString() {
        Node n = new Node("top", 2);
        n.left = new Node("left", 1);
        n.right = new Node("right", 1);
        n.right.left = n;
        n.right.right = n;

        Context ctx = new Context()
                .addFieldAttribute("left", "color=red,fontcolor=red")
                .addFieldAttribute("right", "color=blue,fontcolor=blue")
                .addClassAttribute(Node.class, "color=pink,style=filled")
                .addIgnoreField("level")
                .addIgnoreField("ok")
                .setTreatAsPrimitive(String.class)
                .setShowFieldNamesInLabels(false);

        String actual_graph_5 = ljv.drawGraph(ctx, n);

        String expected_graph_5 = "digraph Java {\n" +
                "n1[label=\"Node|{top}\",color=pink,style=filled,shape=record];\n" +
                "n2[label=\"Node|{left|null|null}\",color=pink,style=filled,shape=record];\n" +
                "n1 -> n2[label=\"left\",fontsize=12,color=red,fontcolor=red];\n" +
                "n3[label=\"Node|{right}\",color=pink,style=filled,shape=record];\n" +
                "n3 -> n1[label=\"left\",fontsize=12,color=red,fontcolor=red];\n" +
                "n3 -> n1[label=\"right\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "n1 -> n3[label=\"right\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "}\n";


        assertEquals(expected_graph_5, actual_graph_5, "Nodes case with context failed");
    }

    @Test
    void cyclicalStructuresClassesWithAndWithoutAToStringAndWithoutContext() {
        Node n = new Node("top", 2);
        n.left = new Node("left", 1);
        n.right = new Node("right", 1);
        n.right.left = n;
        n.right.right = n;

        Context ctx = new Context()
                .addFieldAttribute("left", "color=red,fontcolor=red")
                .addFieldAttribute("right", "color=blue,fontcolor=blue")
                .addClassAttribute(Node.class, "color=pink,style=filled")
                .addIgnoreField("level")
                .addIgnoreField("ok")
                .setTreatAsPrimitive(String.class)
                .setShowFieldNamesInLabels(false);

        String actual_graph_5 = ljv.drawGraph(ctx, n);

        String expected_graph_5 = "digraph Java {\n" +
                "n1[label=\"Node|{top}\",color=pink,style=filled,shape=record];\n" +
                "n2[label=\"Node|{left|null|null}\",color=pink,style=filled,shape=record];\n" +
                "n1 -> n2[label=\"left\",fontsize=12,color=red,fontcolor=red];\n" +
                "n3[label=\"Node|{right}\",color=pink,style=filled,shape=record];\n" +
                "n3 -> n1[label=\"left\",fontsize=12,color=red,fontcolor=red];\n" +
                "n3 -> n1[label=\"right\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "n1 -> n3[label=\"right\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "}\n";


        assertEquals(expected_graph_5, actual_graph_5, "Nodes case without context failed");
    }

    @Test
    void paulsExample() {
        ArrayList<Object> a = new ArrayList<>();
        a.add(new Person("Albert", true, 35));
        a.add(new Person("Betty", false, 20));
        a.add(new java.awt.Point(100, -100));
        String actual_graph_6 = ljv.drawGraph(
                new Context()
                        .addIgnoreField("hash")
                        .addIgnoreField("count")
                        .addIgnoreField("offset")
                , a);

        String expected_graph_6 = "digraph Java {\n" +
                "n1[label=\"java.util.ArrayList|{size: 3}\",shape=record];\n" +
                "n2[label=\"<f0>|<f1>|<f2>|<f3>|<f4>|<f5>|<f6>|<f7>|<f8>|<f9>\",shape=record];\n" +
                "n3[label=\"Person|{isMale: true|age: 35}\",shape=record];\n" +
                "n4[label=\"java.lang.String|{coder: 0}\",shape=record];\n" +
                "n5[shape=record, label=\"65|108|98|101|114|116\"];\n" +
                "n4 -> n5[label=\"value\",fontsize=12];\n" +
                "n3 -> n4[label=\"name\",fontsize=12];\n" +
                "n2:f0 -> n3[label=\"0\",fontsize=12];\n" +
                "n6[label=\"Person|{isMale: false|age: 20}\",shape=record];\n" +
                "n7[label=\"java.lang.String|{coder: 0}\",shape=record];\n" +
                "n8[shape=record, label=\"66|101|116|116|121\"];\n" +
                "n7 -> n8[label=\"value\",fontsize=12];\n" +
                "n6 -> n7[label=\"name\",fontsize=12];\n" +
                "n2:f1 -> n6[label=\"1\",fontsize=12];\n" +
                "n9[label=\"java.awt.Point|{x: 100|y: -100}\",shape=record];\n" +
                "n2:f2 -> n9[label=\"2\",fontsize=12];\n" +
                "n1 -> n2[label=\"elementData\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph_6, actual_graph_6, "Multiarray case failed");
    }
}
