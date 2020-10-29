package org.atpfivt.ljv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.jupiter.api.Disabled;
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
                "\t\t\t\t<td rowspan='3'>java.lang.String</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>coder: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
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
                "\t\t\t\t<td rowspan='3'>java.lang.String</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>coder: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
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
                "\t\t\t\t<td rowspan='3'>java.lang.String</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>coder: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
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
                "\t\t\t\t<td rowspan='3'>java.lang.String</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>coder: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
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
    void cyclicalStructuresClassesWithAndWithoutAToStringAndWithoutContext() {
        Node n1 = new Node("A");
        n1.level = 1;
        AnotherNode n2 = new AnotherNode("B");
        n2.level = 2;
        AnotherNode n3 = new AnotherNode("C");
        n3.level = 2;

        n1.left = n2;
        n1.right = n3;
        n1.right.left = n1;
        n1.right.right = n1;

        String actual_graph = new LJV()
                .addFieldAttribute("left", "color=red,fontcolor=red")
                .addFieldAttribute("right", "color=blue,fontcolor=blue")
                .addClassAttribute(Node.class, "color=pink,style=filled")
                .addIgnoreField("level")
                .setTreatAsPrimitive(String.class)
                .setShowFieldNamesInLabels(false)
                .drawGraph(n1);

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>A</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>,color=pink,style=filled];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>AnotherNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>B</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1 -> n2[label=\"left\",fontsize=12,color=red,fontcolor=red];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>AnotherNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>C</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3 -> n1[label=\"left\",fontsize=12,color=red,fontcolor=red];\n" +
                "\tn3 -> n1[label=\"right\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "\tn1 -> n3[label=\"right\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "}\n";

        assertEquals(expected_graph, actual_graph, "Nodes case without context failed");
    }

    @Test
    void paulsExample() {
        ArrayList<Object> a = new ArrayList<>();
        a.add(new Person("Albert", Gender.MALE, 35));
        a.add(new Person("Betty", Gender.FEMALE, 20));
        a.add(new java.awt.Point(100, -100));

        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setTreatAsPrimitive(Gender.class)
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
                "\t\t\t\t<td rowspan='2'>java.util.ArrayList</td>\n" +
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
                "\t\t\t\t<td rowspan='4'>Person</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>name: Albert</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>gender: MALE</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>age: 35</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2:f0 -> n3[label=\"0\",fontsize=12];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>Person</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>name: Betty</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>gender: FEMALE</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>age: 20</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2:f1 -> n4[label=\"1\",fontsize=12];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='3'>java.awt.Point</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>x: 100</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>y: -100</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2:f2 -> n5[label=\"2\",fontsize=12];\n" +
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

    @Test
    void treeMap() {
        TreeMap<String, Integer> map = new TreeMap<>();

        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);

        String actualGraph = new LJV()
                .setTreatAsPrimitive(Integer.class)
                .setTreatAsPrimitive(String.class)
                .drawGraph(map);

        assertEquals("digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='7'>java.util.TreeMap</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>comparator: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>size: 4</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>modCount: 4</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>entrySet: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>navigableKeySet: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>descendingMap: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='5'>java.util.TreeMap$Entry</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: three</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 3</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>parent: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>color: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='5'>java.util.TreeMap$Entry</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: one</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 1</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>color: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='6'>java.util.TreeMap$Entry</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: four</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 4</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>color: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4 -> n3[label=\"parent\",fontsize=12];\n" +
                "\tn3 -> n4[label=\"left\",fontsize=12];\n" +
                "\tn3 -> n2[label=\"parent\",fontsize=12];\n" +
                "\tn2 -> n3[label=\"left\",fontsize=12];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='6'>java.util.TreeMap$Entry</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: two</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 2</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>color: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn5 -> n2[label=\"parent\",fontsize=12];\n" +
                "\tn2 -> n5[label=\"right\",fontsize=12];\n" +
                "\tn1 -> n2[label=\"root\",fontsize=12];\n" +
                "}\n", actualGraph);
    }

    @Test
    @Disabled
    void concurrentSkipListMap() {
        ConcurrentSkipListMap<String, Integer> map = new ConcurrentSkipListMap<>();

        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);

        String actual_graph = new LJV()
                .setTreatAsPrimitive(Integer.class)
                .setTreatAsPrimitive(String.class)
                .drawGraph(map);

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='6'>java.util.concurrent.ConcurrentSkipListMap</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>comparator: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>keySet: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>values: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>entrySet: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>descendingMap: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>java.util.concurrent.ConcurrentSkipListMap$Index</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='3'>java.util.concurrent.ConcurrentSkipListMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>val: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='3'>java.util.concurrent.ConcurrentSkipListMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: four</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>val: 4</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='3'>java.util.concurrent.ConcurrentSkipListMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: one</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>val: 1</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn6[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='3'>java.util.concurrent.ConcurrentSkipListMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: three</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>val: 3</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn7[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.concurrent.ConcurrentSkipListMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: two</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>val: 2</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>next: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn6 -> n7[label=\"next\",fontsize=12];\n" +
                "\tn5 -> n6[label=\"next\",fontsize=12];\n" +
                "\tn4 -> n5[label=\"next\",fontsize=12];\n" +
                "\tn3 -> n4[label=\"next\",fontsize=12];\n" +
                "\tn2 -> n3[label=\"node\",fontsize=12];\n" +
                "\tn8[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.concurrent.ConcurrentSkipListMap$Index</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>down: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn8 -> n3[label=\"node\",fontsize=12];\n" +
                "\tn9[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='3'>java.util.concurrent.ConcurrentSkipListMap$Index</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>down: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn9 -> n6[label=\"node\",fontsize=12];\n" +
                "\tn8 -> n9[label=\"right\",fontsize=12];\n" +
                "\tn2 -> n8[label=\"down\",fontsize=12];\n" +
                "\tn10[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.concurrent.ConcurrentSkipListMap$Index</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn10 -> n6[label=\"node\",fontsize=12];\n" +
                "\tn10 -> n9[label=\"down\",fontsize=12];\n" +
                "\tn2 -> n10[label=\"right\",fontsize=12];\n" +
                "\tn1 -> n2[label=\"head\",fontsize=12];\n" +
                "\tn11[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>4</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1 -> n11[label=\"adder\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph, actual_graph, "Case with concurrentskiplistmap was failed");
    }

    @Test
    @Disabled
    void linkedHashMap() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);

        String actual_graph = new LJV()
//                .setTreatAsPrimitive(String.class)
//                .setTreatAsPrimitive(Integer.class)
                .drawGraph(map);

        String excepted_graph = "digrah Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.LinkedHashMap</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>accessOrder: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.LinkedHashMap$Entry</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>before: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>two=2</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3 -> n2[label=\"before\",fontsize=12];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>three=3</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4 -> n3[label=\"before\",fontsize=12];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.LinkedHashMap$Entry</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>after: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn5 -> n4[label=\"before\",fontsize=12];\n" +
                "\tn4 -> n5[label=\"after\",fontsize=12];\n" +
                "\tn3 -> n4[label=\"after\",fontsize=12];\n" +
                "\tn2 -> n3[label=\"after\",fontsize=12];\n" +
                "\tn1 -> n2[label=\"head\",fontsize=12];\n" +
                "\tn1 -> n5[label=\"tail\",fontsize=12];\n" +
                "}\n";

        assertEquals(excepted_graph, actual_graph, "Case with linkedHashMap was failed");
    }

    @Test
    void hashMap() {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);

        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setTreatAsPrimitive(Integer.class)
                .drawGraph(map);

        String excepted_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='6'>java.util.HashMap</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>entrySet: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>size: 4</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>modCount: 4</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>threshold: 12</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>loadFactor: 0.75</td>\n" +
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
                "\t\t\t\t<td port=\"f10\"></td>\n" +
                "\t\t\t\t<td port=\"f11\"></td>\n" +
                "\t\t\t\t<td port=\"f12\"></td>\n" +
                "\t\t\t\t<td port=\"f13\"></td>\n" +
                "\t\t\t\t<td port=\"f14\"></td>\n" +
                "\t\t\t\t<td port=\"f15\"></td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='5'>java.util.HashMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>hash: 3149078</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: four</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 4</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>next: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2:f6 -> n3[label=\"6\",fontsize=12];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='5'>java.util.HashMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>hash: 110183</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: one</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 1</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>next: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2:f7 -> n4[label=\"7\",fontsize=12];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>hash: 115277</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: two</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 2</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn6[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='5'>java.util.HashMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>hash: 110338829</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: three</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 3</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>next: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn5 -> n6[label=\"next\",fontsize=12];\n" +
                "\tn2:f13 -> n5[label=\"13\",fontsize=12];\n" +
                "\tn1 -> n2[label=\"table\",fontsize=12];\n" +
                "}\n";

        assertEquals(excepted_graph, actual_graph, "Case with HashMap was failed");
    }

    @Test
    void hashMapCollision2() {
        List<String> collisionString = new HashCodeCollision().genCollisionString(3);
        HashMap<String, Integer> map = new HashMap<>();

        for (int i = 0; i < collisionString.size(); i++) {
            map.put(collisionString.get(i), i);
        }

        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setTreatAsPrimitive(Integer.class)
                .drawGraph(map);

        String excepted_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='6'>java.util.HashMap</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>entrySet: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>size: 3</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>modCount: 3</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>threshold: 12</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>loadFactor: 0.75</td>\n" +
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
                "\t\t\t\t<td port=\"f10\"></td>\n" +
                "\t\t\t\t<td port=\"f11\"></td>\n" +
                "\t\t\t\t<td port=\"f12\"></td>\n" +
                "\t\t\t\t<td port=\"f13\"></td>\n" +
                "\t\t\t\t<td port=\"f14\"></td>\n" +
                "\t\t\t\t<td port=\"f15\"></td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>hash: 96320</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: aaa</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 0</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>hash: 96320</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: abB</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 1</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='5'>java.util.HashMap$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>hash: 96320</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>key: bBa</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 2</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>next: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4 -> n5[label=\"next\",fontsize=12];\n" +
                "\tn3 -> n4[label=\"next\",fontsize=12];\n" +
                "\tn2:f0 -> n3[label=\"0\",fontsize=12];\n" +
                "\tn1 -> n2[label=\"table\",fontsize=12];\n" +
                "}\n";

        assertEquals(excepted_graph, actual_graph, "Case with hashMapCollision was failed");
    }


    @Test
    void hashMapCollision3() {
        List<String> collisionString = new HashCodeCollision().genCollisionString(6);
        HashMap<String, Integer> map = new HashMap<>();

        for (int i = 0; i < collisionString.size(); i++) {
            map.put(collisionString.get(i), i);
        }

        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setTreatAsPrimitive(Integer.class)
                .drawGraph(map);

        String excepted_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='6'>java.util.HashMap</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>entrySet: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>size: 13</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>modCount: 13</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>threshold: 48</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>loadFactor: 0.75</td>\n" +
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
                "\t\t\t\t<td port=\"f10\"></td>\n" +
                "\t\t\t\t<td port=\"f11\"></td>\n" +
                "\t\t\t\t<td port=\"f12\"></td>\n" +
                "\t\t\t\t<td port=\"f13\"></td>\n" +
                "\t\t\t\t<td port=\"f14\"></td>\n" +
                "\t\t\t\t<td port=\"f15\"></td>\n" +
                "\t\t\t\t<td port=\"f16\"></td>\n" +
                "\t\t\t\t<td port=\"f17\"></td>\n" +
                "\t\t\t\t<td port=\"f18\"></td>\n" +
                "\t\t\t\t<td port=\"f19\"></td>\n" +
                "\t\t\t\t<td port=\"f20\"></td>\n" +
                "\t\t\t\t<td port=\"f21\"></td>\n" +
                "\t\t\t\t<td port=\"f22\"></td>\n" +
                "\t\t\t\t<td port=\"f23\"></td>\n" +
                "\t\t\t\t<td port=\"f24\"></td>\n" +
                "\t\t\t\t<td port=\"f25\"></td>\n" +
                "\t\t\t\t<td port=\"f26\"></td>\n" +
                "\t\t\t\t<td port=\"f27\"></td>\n" +
                "\t\t\t\t<td port=\"f28\"></td>\n" +
                "\t\t\t\t<td port=\"f29\"></td>\n" +
                "\t\t\t\t<td port=\"f30\"></td>\n" +
                "\t\t\t\t<td port=\"f31\"></td>\n" +
                "\t\t\t\t<td port=\"f32\"></td>\n" +
                "\t\t\t\t<td port=\"f33\"></td>\n" +
                "\t\t\t\t<td port=\"f34\"></td>\n" +
                "\t\t\t\t<td port=\"f35\"></td>\n" +
                "\t\t\t\t<td port=\"f36\"></td>\n" +
                "\t\t\t\t<td port=\"f37\"></td>\n" +
                "\t\t\t\t<td port=\"f38\"></td>\n" +
                "\t\t\t\t<td port=\"f39\"></td>\n" +
                "\t\t\t\t<td port=\"f40\"></td>\n" +
                "\t\t\t\t<td port=\"f41\"></td>\n" +
                "\t\t\t\t<td port=\"f42\"></td>\n" +
                "\t\t\t\t<td port=\"f43\"></td>\n" +
                "\t\t\t\t<td port=\"f44\"></td>\n" +
                "\t\t\t\t<td port=\"f45\"></td>\n" +
                "\t\t\t\t<td port=\"f46\"></td>\n" +
                "\t\t\t\t<td port=\"f47\"></td>\n" +
                "\t\t\t\t<td port=\"f48\"></td>\n" +
                "\t\t\t\t<td port=\"f49\"></td>\n" +
                "\t\t\t\t<td port=\"f50\"></td>\n" +
                "\t\t\t\t<td port=\"f51\"></td>\n" +
                "\t\t\t\t<td port=\"f52\"></td>\n" +
                "\t\t\t\t<td port=\"f53\"></td>\n" +
                "\t\t\t\t<td port=\"f54\"></td>\n" +
                "\t\t\t\t<td port=\"f55\"></td>\n" +
                "\t\t\t\t<td port=\"f56\"></td>\n" +
                "\t\t\t\t<td port=\"f57\"></td>\n" +
                "\t\t\t\t<td port=\"f58\"></td>\n" +
                "\t\t\t\t<td port=\"f59\"></td>\n" +
                "\t\t\t\t<td port=\"f60\"></td>\n" +
                "\t\t\t\t<td port=\"f61\"></td>\n" +
                "\t\t\t\t<td port=\"f62\"></td>\n" +
                "\t\t\t\t<td port=\"f63\"></td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>parent: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>prev: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4 -> n3[label=\"parent\",fontsize=12];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn5 -> n4[label=\"parent\",fontsize=12];\n" +
                "\tn5 -> n3[label=\"prev\",fontsize=12];\n" +
                "\tn4 -> n5[label=\"left\",fontsize=12];\n" +
                "\tn6[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn6 -> n4[label=\"parent\",fontsize=12];\n" +
                "\tn6 -> n4[label=\"prev\",fontsize=12];\n" +
                "\tn4 -> n6[label=\"right\",fontsize=12];\n" +
                "\tn4 -> n5[label=\"prev\",fontsize=12];\n" +
                "\tn3 -> n4[label=\"left\",fontsize=12];\n" +
                "\tn7[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn7 -> n3[label=\"parent\",fontsize=12];\n" +
                "\tn8[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn8 -> n7[label=\"parent\",fontsize=12];\n" +
                "\tn9[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn9 -> n8[label=\"parent\",fontsize=12];\n" +
                "\tn9 -> n6[label=\"prev\",fontsize=12];\n" +
                "\tn8 -> n9[label=\"left\",fontsize=12];\n" +
                "\tn10[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn10 -> n8[label=\"parent\",fontsize=12];\n" +
                "\tn10 -> n8[label=\"prev\",fontsize=12];\n" +
                "\tn8 -> n10[label=\"right\",fontsize=12];\n" +
                "\tn8 -> n9[label=\"prev\",fontsize=12];\n" +
                "\tn7 -> n8[label=\"left\",fontsize=12];\n" +
                "\tn11[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn11 -> n7[label=\"parent\",fontsize=12];\n" +
                "\tn12[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn12 -> n11[label=\"parent\",fontsize=12];\n" +
                "\tn12 -> n7[label=\"prev\",fontsize=12];\n" +
                "\tn11 -> n12[label=\"left\",fontsize=12];\n" +
                "\tn13[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn13 -> n11[label=\"parent\",fontsize=12];\n" +
                "\tn14[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn14 -> n13[label=\"parent\",fontsize=12];\n" +
                "\tn14 -> n11[label=\"prev\",fontsize=12];\n" +
                "\tn13 -> n14[label=\"left\",fontsize=12];\n" +
                "\tn15[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn15 -> n13[label=\"parent\",fontsize=12];\n" +
                "\tn15 -> n13[label=\"prev\",fontsize=12];\n" +
                "\tn13 -> n15[label=\"right\",fontsize=12];\n" +
                "\tn13 -> n14[label=\"prev\",fontsize=12];\n" +
                "\tn11 -> n13[label=\"right\",fontsize=12];\n" +
                "\tn11 -> n12[label=\"prev\",fontsize=12];\n" +
                "\tn7 -> n11[label=\"right\",fontsize=12];\n" +
                "\tn7 -> n10[label=\"prev\",fontsize=12];\n" +
                "\tn3 -> n7[label=\"right\",fontsize=12];\n" +
                "\tn2:f27 -> n3[label=\"27\",fontsize=12];\n" +
                "\tn1 -> n2[label=\"table\",fontsize=12];\n" +
                "}\n";

        assertEquals(excepted_graph, actual_graph, "Case with hashMapCollision was failed");
    }

    @Test
    void hashMapCollision4() {
        List<String> collisionString = new HashCodeCollision().genCollisionString(8);
        HashMap<String, Integer> map = new HashMap<>();

        for (int i = 0; i < collisionString.size(); i++) {
            map.put(collisionString.get(i), i);
        }

        String actual_graph = new LJV()
                .setTreatAsPrimitive(String.class)
                .setTreatAsPrimitive(Integer.class)
                .drawGraph(map);

        String excepted_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='6'>java.util.HashMap</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>entrySet: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>size: 34</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>modCount: 34</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>threshold: 48</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>loadFactor: 0.75</td>\n" +
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
                "\t\t\t\t<td port=\"f10\"></td>\n" +
                "\t\t\t\t<td port=\"f11\"></td>\n" +
                "\t\t\t\t<td port=\"f12\"></td>\n" +
                "\t\t\t\t<td port=\"f13\"></td>\n" +
                "\t\t\t\t<td port=\"f14\"></td>\n" +
                "\t\t\t\t<td port=\"f15\"></td>\n" +
                "\t\t\t\t<td port=\"f16\"></td>\n" +
                "\t\t\t\t<td port=\"f17\"></td>\n" +
                "\t\t\t\t<td port=\"f18\"></td>\n" +
                "\t\t\t\t<td port=\"f19\"></td>\n" +
                "\t\t\t\t<td port=\"f20\"></td>\n" +
                "\t\t\t\t<td port=\"f21\"></td>\n" +
                "\t\t\t\t<td port=\"f22\"></td>\n" +
                "\t\t\t\t<td port=\"f23\"></td>\n" +
                "\t\t\t\t<td port=\"f24\"></td>\n" +
                "\t\t\t\t<td port=\"f25\"></td>\n" +
                "\t\t\t\t<td port=\"f26\"></td>\n" +
                "\t\t\t\t<td port=\"f27\"></td>\n" +
                "\t\t\t\t<td port=\"f28\"></td>\n" +
                "\t\t\t\t<td port=\"f29\"></td>\n" +
                "\t\t\t\t<td port=\"f30\"></td>\n" +
                "\t\t\t\t<td port=\"f31\"></td>\n" +
                "\t\t\t\t<td port=\"f32\"></td>\n" +
                "\t\t\t\t<td port=\"f33\"></td>\n" +
                "\t\t\t\t<td port=\"f34\"></td>\n" +
                "\t\t\t\t<td port=\"f35\"></td>\n" +
                "\t\t\t\t<td port=\"f36\"></td>\n" +
                "\t\t\t\t<td port=\"f37\"></td>\n" +
                "\t\t\t\t<td port=\"f38\"></td>\n" +
                "\t\t\t\t<td port=\"f39\"></td>\n" +
                "\t\t\t\t<td port=\"f40\"></td>\n" +
                "\t\t\t\t<td port=\"f41\"></td>\n" +
                "\t\t\t\t<td port=\"f42\"></td>\n" +
                "\t\t\t\t<td port=\"f43\"></td>\n" +
                "\t\t\t\t<td port=\"f44\"></td>\n" +
                "\t\t\t\t<td port=\"f45\"></td>\n" +
                "\t\t\t\t<td port=\"f46\"></td>\n" +
                "\t\t\t\t<td port=\"f47\"></td>\n" +
                "\t\t\t\t<td port=\"f48\"></td>\n" +
                "\t\t\t\t<td port=\"f49\"></td>\n" +
                "\t\t\t\t<td port=\"f50\"></td>\n" +
                "\t\t\t\t<td port=\"f51\"></td>\n" +
                "\t\t\t\t<td port=\"f52\"></td>\n" +
                "\t\t\t\t<td port=\"f53\"></td>\n" +
                "\t\t\t\t<td port=\"f54\"></td>\n" +
                "\t\t\t\t<td port=\"f55\"></td>\n" +
                "\t\t\t\t<td port=\"f56\"></td>\n" +
                "\t\t\t\t<td port=\"f57\"></td>\n" +
                "\t\t\t\t<td port=\"f58\"></td>\n" +
                "\t\t\t\t<td port=\"f59\"></td>\n" +
                "\t\t\t\t<td port=\"f60\"></td>\n" +
                "\t\t\t\t<td port=\"f61\"></td>\n" +
                "\t\t\t\t<td port=\"f62\"></td>\n" +
                "\t\t\t\t<td port=\"f63\"></td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>parent: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>prev: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4 -> n3[label=\"parent\",fontsize=12];\n" +
                "\tn5[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn5 -> n4[label=\"parent\",fontsize=12];\n" +
                "\tn6[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn6 -> n5[label=\"parent\",fontsize=12];\n" +
                "\tn6 -> n4[label=\"prev\",fontsize=12];\n" +
                "\tn5 -> n6[label=\"left\",fontsize=12];\n" +
                "\tn7[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn7 -> n5[label=\"parent\",fontsize=12];\n" +
                "\tn7 -> n5[label=\"prev\",fontsize=12];\n" +
                "\tn5 -> n7[label=\"right\",fontsize=12];\n" +
                "\tn5 -> n6[label=\"prev\",fontsize=12];\n" +
                "\tn4 -> n5[label=\"left\",fontsize=12];\n" +
                "\tn8[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn8 -> n4[label=\"parent\",fontsize=12];\n" +
                "\tn9[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn9 -> n8[label=\"parent\",fontsize=12];\n" +
                "\tn9 -> n7[label=\"prev\",fontsize=12];\n" +
                "\tn8 -> n9[label=\"left\",fontsize=12];\n" +
                "\tn10[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn10 -> n8[label=\"parent\",fontsize=12];\n" +
                "\tn10 -> n8[label=\"prev\",fontsize=12];\n" +
                "\tn8 -> n10[label=\"right\",fontsize=12];\n" +
                "\tn8 -> n9[label=\"prev\",fontsize=12];\n" +
                "\tn4 -> n8[label=\"right\",fontsize=12];\n" +
                "\tn4 -> n3[label=\"prev\",fontsize=12];\n" +
                "\tn3 -> n4[label=\"left\",fontsize=12];\n" +
                "\tn11[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn11 -> n3[label=\"parent\",fontsize=12];\n" +
                "\tn12[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn12 -> n11[label=\"parent\",fontsize=12];\n" +
                "\tn13[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn13 -> n12[label=\"parent\",fontsize=12];\n" +
                "\tn14[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn14 -> n13[label=\"parent\",fontsize=12];\n" +
                "\tn14 -> n10[label=\"prev\",fontsize=12];\n" +
                "\tn13 -> n14[label=\"left\",fontsize=12];\n" +
                "\tn15[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn15 -> n13[label=\"parent\",fontsize=12];\n" +
                "\tn15 -> n13[label=\"prev\",fontsize=12];\n" +
                "\tn13 -> n15[label=\"right\",fontsize=12];\n" +
                "\tn13 -> n14[label=\"prev\",fontsize=12];\n" +
                "\tn12 -> n13[label=\"left\",fontsize=12];\n" +
                "\tn16[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn16 -> n12[label=\"parent\",fontsize=12];\n" +
                "\tn17[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn17 -> n16[label=\"parent\",fontsize=12];\n" +
                "\tn17 -> n12[label=\"prev\",fontsize=12];\n" +
                "\tn16 -> n17[label=\"left\",fontsize=12];\n" +
                "\tn18[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn18 -> n16[label=\"parent\",fontsize=12];\n" +
                "\tn18 -> n16[label=\"prev\",fontsize=12];\n" +
                "\tn16 -> n18[label=\"right\",fontsize=12];\n" +
                "\tn16 -> n17[label=\"prev\",fontsize=12];\n" +
                "\tn12 -> n16[label=\"right\",fontsize=12];\n" +
                "\tn12 -> n15[label=\"prev\",fontsize=12];\n" +
                "\tn11 -> n12[label=\"left\",fontsize=12];\n" +
                "\tn19[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn19 -> n11[label=\"parent\",fontsize=12];\n" +
                "\tn20[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn20 -> n19[label=\"parent\",fontsize=12];\n" +
                "\tn21[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn21 -> n20[label=\"parent\",fontsize=12];\n" +
                "\tn22[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn22 -> n21[label=\"parent\",fontsize=12];\n" +
                "\tn22 -> n11[label=\"prev\",fontsize=12];\n" +
                "\tn21 -> n22[label=\"left\",fontsize=12];\n" +
                "\tn23[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn23 -> n21[label=\"parent\",fontsize=12];\n" +
                "\tn23 -> n21[label=\"prev\",fontsize=12];\n" +
                "\tn21 -> n23[label=\"right\",fontsize=12];\n" +
                "\tn21 -> n22[label=\"prev\",fontsize=12];\n" +
                "\tn20 -> n21[label=\"left\",fontsize=12];\n" +
                "\tn24[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn24 -> n20[label=\"parent\",fontsize=12];\n" +
                "\tn25[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn25 -> n24[label=\"parent\",fontsize=12];\n" +
                "\tn25 -> n20[label=\"prev\",fontsize=12];\n" +
                "\tn24 -> n25[label=\"left\",fontsize=12];\n" +
                "\tn26[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn26 -> n24[label=\"parent\",fontsize=12];\n" +
                "\tn26 -> n24[label=\"prev\",fontsize=12];\n" +
                "\tn24 -> n26[label=\"right\",fontsize=12];\n" +
                "\tn24 -> n25[label=\"prev\",fontsize=12];\n" +
                "\tn20 -> n24[label=\"right\",fontsize=12];\n" +
                "\tn20 -> n23[label=\"prev\",fontsize=12];\n" +
                "\tn19 -> n20[label=\"left\",fontsize=12];\n" +
                "\tn27[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn27 -> n19[label=\"parent\",fontsize=12];\n" +
                "\tn28[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn28 -> n27[label=\"parent\",fontsize=12];\n" +
                "\tn29[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn29 -> n28[label=\"parent\",fontsize=12];\n" +
                "\tn29 -> n19[label=\"prev\",fontsize=12];\n" +
                "\tn28 -> n29[label=\"left\",fontsize=12];\n" +
                "\tn30[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn30 -> n28[label=\"parent\",fontsize=12];\n" +
                "\tn30 -> n28[label=\"prev\",fontsize=12];\n" +
                "\tn28 -> n30[label=\"right\",fontsize=12];\n" +
                "\tn28 -> n29[label=\"prev\",fontsize=12];\n" +
                "\tn27 -> n28[label=\"left\",fontsize=12];\n" +
                "\tn31[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn31 -> n27[label=\"parent\",fontsize=12];\n" +
                "\tn32[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn32 -> n31[label=\"parent\",fontsize=12];\n" +
                "\tn32 -> n27[label=\"prev\",fontsize=12];\n" +
                "\tn31 -> n32[label=\"left\",fontsize=12];\n" +
                "\tn33[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn33 -> n31[label=\"parent\",fontsize=12];\n" +
                "\tn34[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn34 -> n33[label=\"parent\",fontsize=12];\n" +
                "\tn34 -> n31[label=\"prev\",fontsize=12];\n" +
                "\tn33 -> n34[label=\"left\",fontsize=12];\n" +
                "\tn35[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='3'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: false</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn35 -> n33[label=\"parent\",fontsize=12];\n" +
                "\tn36[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='4'>java.util.HashMap$TreeNode</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>left: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>right: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>red: true</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn36 -> n35[label=\"parent\",fontsize=12];\n" +
                "\tn36 -> n35[label=\"prev\",fontsize=12];\n" +
                "\tn35 -> n36[label=\"right\",fontsize=12];\n" +
                "\tn35 -> n33[label=\"prev\",fontsize=12];\n" +
                "\tn33 -> n35[label=\"right\",fontsize=12];\n" +
                "\tn33 -> n34[label=\"prev\",fontsize=12];\n" +
                "\tn31 -> n33[label=\"right\",fontsize=12];\n" +
                "\tn31 -> n32[label=\"prev\",fontsize=12];\n" +
                "\tn27 -> n31[label=\"right\",fontsize=12];\n" +
                "\tn27 -> n30[label=\"prev\",fontsize=12];\n" +
                "\tn19 -> n27[label=\"right\",fontsize=12];\n" +
                "\tn19 -> n26[label=\"prev\",fontsize=12];\n" +
                "\tn11 -> n19[label=\"right\",fontsize=12];\n" +
                "\tn11 -> n18[label=\"prev\",fontsize=12];\n" +
                "\tn3 -> n11[label=\"right\",fontsize=12];\n" +
                "\tn2:f15 -> n3[label=\"15\",fontsize=12];\n" +
                "\tn1 -> n2[label=\"table\",fontsize=12];\n" +
                "}\n";

        assertEquals(excepted_graph, actual_graph, "Case with hashMapCollision was failed");
    }

    @Test
    void wrappedObjects() {
        String actual_graph = new LJV().drawGraph(new Example());

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>Example</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.lang.Integer</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 42</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1 -> n2[label=\"i1\",fontsize=12];\n" +
                "\tn1 -> n2[label=\"i2\",fontsize=12];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.lang.Integer</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 2020</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1 -> n3[label=\"i3\",fontsize=12];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.lang.Integer</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>value: 2020</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1 -> n4[label=\"i4\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph, actual_graph, "Case with wrapped objects was failed");
    }

    @Test
    void linkedList() {
        LinkedList<Integer> linkedList = new LinkedList<>();
        linkedList.add(1);
        linkedList.add(42);
        linkedList.add(21);

        String actual_graph = new LJV()
                .setTreatAsPrimitive(Integer.class)
                .addFieldAttribute("next", "color=red,fontcolor=red")
                .addFieldAttribute("prev", "color=blue,fontcolor=blue")
                .addFieldAttribute("first", "color=red,fontcolor=red")
                .addFieldAttribute("last", "color=red,fontcolor=red")
                .drawGraph(linkedList);

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.LinkedList</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>size: 3</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='3'>java.util.LinkedList$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>item: 1</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>prev: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn3[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='2'>java.util.LinkedList$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>item: 42</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='3'>java.util.LinkedList$Node</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>item: 21</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>next: null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn4 -> n3[label=\"prev\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "\tn3 -> n4[label=\"next\",fontsize=12,color=red,fontcolor=red];\n" +
                "\tn3 -> n2[label=\"prev\",fontsize=12,color=blue,fontcolor=blue];\n" +
                "\tn2 -> n3[label=\"next\",fontsize=12,color=red,fontcolor=red];\n" +
                "\tn1 -> n2[label=\"first\",fontsize=12,color=red,fontcolor=red];\n" +
                "\tn1 -> n4[label=\"last\",fontsize=12,color=red,fontcolor=red];\n" +
                "}\n";

        assertEquals(expected_graph, actual_graph, "Case with linked list was failed");
    }

    @Test
    void arrayDeque() {
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<>();
        for (int i = 0; i < 20; i++) {
            arrayDeque.addLast(i);
        }
        for (int i = 0; i < 18; i++) {
            arrayDeque.removeFirst();
        }

        String actual_graph = new LJV()
                .setTreatAsPrimitive(Integer.class).drawGraph(arrayDeque);

        String expected_graph = "digraph Java {\n" +
                "\trankdir=\"TB\";\n" +
                "\tnode[shape=plaintext]\n" +
                "\tn1[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td rowspan='3'>java.util.ArrayDeque</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>head: 2</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>tail: 4</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn2[label=<\n" +
                "\t\t<table border='0' cellborder='1' cellspacing='0'>\n" +
                "\t\t\t<tr>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>18</td>\n" +
                "\t\t\t\t<td>19</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t\t<td>null</td>\n" +
                "\t\t\t</tr>\n" +
                "\t\t</table>\n" +
                "\t>];\n" +
                "\tn1 -> n2[label=\"elements\",fontsize=12];\n" +
                "}\n";

        assertEquals(expected_graph, actual_graph, "Case with arrayDeque was failed");
    }
}
