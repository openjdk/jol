package org.atpfivt.ljv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

public class LJVTest {

    @Test
    void stringIsNotAPrimitiveType() {
        String actual_graph = new LJV().drawGraph("Hello");

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='2'>java.lang.String</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>coder: 0</td>\n" +
                "\t\t\t\t<td>hash: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>72</td>\n" +
                "\t\t\t\t<td>101</td>\n" +
                "\t\t\t\t<td>108</td>\n" +
                "\t\t\t\t<td>108</td>\n" +
                "\t\t\t\t<td>111</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1 -> n2[label=\"value\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph, actual_graph, "Hello case failed");
    }

    @Test
    void objectArraysHoldReferencesPrimitiveArraysHoldValues() {
        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setIgnorePrivateFields(false)
                .drawGraph(
                new Object[]{new String[]{"a", "b", "c"}, new int[]{1, 2, 3}}
                );

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0' cellpadding='9'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td port=\"f0\"></td>\n" +
                "\t\t\t\t<td port=\"f1\"></td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>a</td>\n" +
                "\t\t\t\t<td>b</td>\n" +
                "\t\t\t\t<td>c</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1:f0 -> n2[label=\"0\",fontsize=12];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>1</td>\n" +
                "\t\t\t\t<td>2</td>\n" +
                "\t\t\t\t<td>3</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1:f1 -> n3[label=\"1\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph, actual_graph, "Primitive array case failed");
    }

    @Test
    void assignmentDoesNotCreateANewObject() {
        String x = "Hello";
        String y = x;
        String actual_graph = new LJV().drawGraph(new Object[]{x, y});

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0' cellpadding='9'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td port=\"f0\"></td>\n" +
                "\t\t\t\t<td port=\"f1\"></td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='2'>java.lang.String</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>coder: 0</td>\n" +
                "\t\t\t\t<td>hash: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>72</td>\n" +
                "\t\t\t\t<td>101</td>\n" +
                "\t\t\t\t<td>108</td>\n" +
                "\t\t\t\t<td>108</td>\n" +
                "\t\t\t\t<td>111</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2 -> n3[label=\"value\",fontsize=12];\n" +
                "\tn1:f0 -> n2[label=\"0\",fontsize=12];\n" +
                "\tn1:f1 -> n2[label=\"1\",fontsize=12];\n" +
                "}\n";


        assertEquals(expected_graph, actual_graph, "One link Hello case failed");
    }

    @Test
    void assignmentWithNewCreateANewObject() {
        String x = "Hello";
        String y = new String(x);
        String actual_graph = new LJV().drawGraph(new Object[]{x, y});

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0' cellpadding='9'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td port=\"f0\"></td>\n" +
                "\t\t\t\t<td port=\"f1\"></td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='2'>java.lang.String</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>coder: 0</td>\n" +
                "\t\t\t\t<td>hash: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>72</td>\n" +
                "\t\t\t\t<td>101</td>\n" +
                "\t\t\t\t<td>108</td>\n" +
                "\t\t\t\t<td>108</td>\n" +
                "\t\t\t\t<td>111</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2 -> n3[label=\"value\",fontsize=12];\n" +
                "\tn1:f0 -> n2[label=\"0\",fontsize=12];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='2'>java.lang.String</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>coder: 0</td>\n" +
                "\t\t\t\t<td>hash: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4 -> n3[label=\"value\",fontsize=12];\n" +
                "\tn1:f1 -> n4[label=\"1\",fontsize=12];\n" +
                "}\n";


        assertEquals(expected_graph, actual_graph, "Without duplicate hello case failed");
    }

    @Test
    void multiDimensionalArrays() {
        String actual_graph = new LJV().drawGraph(new int[4][5]);

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0' cellpadding='9'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td port=\"f0\"></td>\n" +
                "\t\t\t\t<td port=\"f1\"></td>\n" +
                "\t\t\t\t<td port=\"f2\"></td>\n" +
                "\t\t\t\t<td port=\"f3\"></td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1:f0 -> n2[label=\"0\",fontsize=12];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1:f1 -> n3[label=\"1\",fontsize=12];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1:f2 -> n4[label=\"2\",fontsize=12];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1:f3 -> n5[label=\"3\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph, actual_graph, "Multiarray case failed");
    }

    @Test
    void reversedMultiDimensionalArrays() {
        String actual_graph = new LJV().setDirection(Direction.LR).drawGraph(new int[4][5]);

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"LR\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0' cellpadding='9'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td port=\"f0\"></td>\n" +
                "\t\t\t\t<td port=\"f1\"></td>\n" +
                "\t\t\t\t<td port=\"f2\"></td>\n" +
                "\t\t\t\t<td port=\"f3\"></td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1:f0 -> n2[label=\"0\",fontsize=12];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1:f1 -> n3[label=\"1\",fontsize=12];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1:f2 -> n4[label=\"2\",fontsize=12];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t\t<td>0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1:f3 -> n5[label=\"3\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph, actual_graph, "Multiarray case failed");
    }

    @Test
    void cyclicalStructuresClassesWithAndWithoutAToString() {
        Node n = new Node("top", 2);
        n.left = new Node("left", 1);
        n.right = new Node("right", 1);
        n.right.left = n;
        n.right.right = n;

        String actual_graph = new LJV()
                .addFieldAttribute("left", "color=red,fontcolor=red")
                .addFieldAttribute("right", "color=blue,fontcolor=blue")
                .addClassAttribute(Node.class, "color=pink,style=filled")
                .addIgnoreField("level")
                .addIgnoreField("ok")
                .setTreatAsPrimitive(String.class)
                .setShowFieldNamesInLabels(false)
                .drawGraph(n);

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='1'>Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>top</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>,color=pink,style=filled];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='3'>Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>,color=pink,style=filled];\n" +
                "\tn1 -> n2[label=\"left\",fontsize=12,color=red,fontcolor=red];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='1'>Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>,color=pink,style=filled];\n" +
                "\tn3 -> n1[label=\"left\",fontsize=12,color=red,fontcolor=red];\n" +
                "\tn3 -> n1[label=\"right\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "\tn1 -> n3[label=\"right\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "}\n";


        assertEquals(expected_graph, actual_graph, "Nodes case with context failed");
    }

    @Test
    void cyclicalStructuresClassesWithAndWithoutAToStringAndWithoutContext() {
        Node n = new Node("top", 2);
        n.left = new Node("left", 1);
        n.right = new Node("right", 1);
        n.right.left = n;
        n.right.right = n;

        String actual_graph = new LJV()
                .addFieldAttribute("left", "color=red,fontcolor=red")
                .addFieldAttribute("right", "color=blue,fontcolor=blue")
                .addClassAttribute(Node.class, "color=pink,style=filled")
                .addIgnoreField("level")
                .addIgnoreField("ok")
                .setTreatAsPrimitive(String.class)
                .setShowFieldNamesInLabels(false)
                .drawGraph(n);

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='1'>Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>top</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>,color=pink,style=filled];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='3'>Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>,color=pink,style=filled];\n" +
                "\tn1 -> n2[label=\"left\",fontsize=12,color=red,fontcolor=red];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='1'>Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>,color=pink,style=filled];\n" +
                "\tn3 -> n1[label=\"left\",fontsize=12,color=red,fontcolor=red];\n" +
                "\tn3 -> n1[label=\"right\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "\tn1 -> n3[label=\"right\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "}\n";


        assertEquals(expected_graph, actual_graph, "Nodes case without context failed");
    }

    @Test
    void paulsExample() {
        ArrayList<Object> a = new ArrayList<>();
        a.add(new Person("Albert", true, 35));
        a.add(new Person("Betty", false, 20));
        a.add(new java.awt.Point(100, -100));

        String actual_graph = new LJV()
                .addIgnoreField("hash")
                .addIgnoreField("count")
                .addIgnoreField("offset")
                .drawGraph(a);

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='1'>java.util.ArrayList</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>size: 3</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0' cellpadding='9'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td port=\"f0\"></td>\n" +
                "\t\t\t\t<td port=\"f1\"></td>\n" +
                "\t\t\t\t<td port=\"f2\"></td>\n" +
                "\t\t\t\t<td port=\"f3\"></td>\n" +
                "\t\t\t\t<td port=\"f4\"></td>\n" +
                "\t\t\t\t<td port=\"f5\"></td>\n" +
                "\t\t\t\t<td port=\"f6\"></td>\n" +
                "\t\t\t\t<td port=\"f7\"></td>\n" +
                "\t\t\t\t<td port=\"f8\"></td>\n" +
                "\t\t\t\t<td port=\"f9\"></td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='2'>Person</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>isMale: true</td>\n" +
                "\t\t\t\t<td>age: 35</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='1'>java.lang.String</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>coder: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>65</td>\n" +
                "\t\t\t\t<td>108</td>\n" +
                "\t\t\t\t<td>98</td>\n" +
                "\t\t\t\t<td>101</td>\n" +
                "\t\t\t\t<td>114</td>\n" +
                "\t\t\t\t<td>116</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4 -> n5[label=\"value\",fontsize=12];\n" +
                "\tn3 -> n4[label=\"name\",fontsize=12];\n" +
                "\tn2:f0 -> n3[label=\"0\",fontsize=12];\n" +
                "\tn6[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='2'>Person</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>isMale: false</td>\n" +
                "\t\t\t\t<td>age: 20</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn7[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='1'>java.lang.String</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>coder: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn8[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>66</td>\n" +
                "\t\t\t\t<td>101</td>\n" +
                "\t\t\t\t<td>116</td>\n" +
                "\t\t\t\t<td>116</td>\n" +
                "\t\t\t\t<td>121</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn7 -> n8[label=\"value\",fontsize=12];\n" +
                "\tn6 -> n7[label=\"name\",fontsize=12];\n" +
                "\tn2:f1 -> n6[label=\"1\",fontsize=12];\n" +
                "\tn9[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td colspan='2'>java.awt.Point</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>x: 100</td>\n" +
                "\t\t\t\t<td>y: -100</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2:f2 -> n9[label=\"2\",fontsize=12];\n" +
                "\tn1 -> n2[label=\"elementData\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph, actual_graph, "Multiarray case failed");
    }

    @Test
    void testNull() {
        String actualGraph = new LJV().drawGraph(null);

        assertEquals("digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tNULL[label=\"null\", shape=plaintext];\n" +
                "}\n", actualGraph);
    }

}
