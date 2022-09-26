/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jol.ljv;

//- Author:     John Hamer <J.Hamer@cs.auckland.ac.nz>
//- Created:    Sat May 10 15:27:48 2003

import org.openjdk.jol.ljv.nodes.Node;
import org.openjdk.jol.ljv.provider.ArrayElementAttributeProvider;
import org.openjdk.jol.ljv.provider.FieldAttributesProvider;
import org.openjdk.jol.ljv.provider.ObjectAttributesProvider;
import org.openjdk.jol.ljv.provider.impl.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Lightweight Java Visualizer.
 */
public final class LJV {
    private final List<ObjectAttributesProvider> objectAttributesProviders = new ArrayList<>();
    private final List<FieldAttributesProvider> fieldAttributesProviders = new ArrayList<>();
    private final List<ArrayElementAttributeProvider> arrayElementAttributeProviders = new ArrayList<>();
    private final Set<Object> pretendPrimitiveSet = new HashSet<>();
    private final Set<Object> ignoreSet = new HashSet<>();
    private final List<Object> roots = new ArrayList<>();
    private Direction direction = Direction.TB;

    public LJV setDirection(Direction direction) {
        this.direction = direction;
        return this;
    }

    public Direction getDirection() {
        return direction;
    }

    private enum Options {
        /**
         * Allow private, protected and package-access fields to be shown.
         * This is only possible if the security manager allows
         * <code>ReflectPermission("suppressAccessChecks")</code> permission.
         * This is usually the case when running from an application, but
         * not from an applet or servlet.
         */
        IGNOREPRIVATEFIELDS,
        /**
         * Toggle whether to display the class name in the label for an
         * object (false, the default) or to use the result of calling
         * toString (true).
         */
        USETOSTRINGASCLASSNAME,
        /**
         * Toggle whether to display qualified nested class names in the
         * label for an object from the same package as LJV (true) or
         * to display an abbreviated name (false, the default).
         */
        QUALIFYNESTEDCLASSNAMES,
        SHOWPACKAGENAMESINCLASSES,
        /**
         * Toggle whether or not to include the field name in the label for an
         * object.  This is currently all-or-nothing.  TODO: allow this to be
         * set on a per-class basis.
         */
        SHOWFIELDNAMESINLABELS,
        /**
         * Toggle whether to ignore fields with null values
         */
        IGNORENULLVALUEDFIELDS,
    }

    private final EnumSet<Options> oSet = EnumSet.of(Options.SHOWFIELDNAMESINLABELS);

    /**
     * Set the DOT attributes for a class.  This allows you to change the
     * appearance of certain nodes in the output, but requires that you
     * know something about dot attributes.  Simple attributes are, e.g.,
     * "color=red".
     *
     * @param cz     class to set attribute for.
     * @param attrib DOT attributes for a class.
     * @return current LJV object
     */
    public LJV addClassAttribute(Class<?> cz, String attrib) {
        objectAttributesProviders.add(new FixedValueClassAttributes(cz, attrib));
        return this;
    }

    public LJV addObjectAttributesProvider(ObjectAttributesProvider provider) {
        objectAttributesProviders.add(Objects.requireNonNull(provider));
        return this;
    }

    public String getObjectAttributes(Object o) {
        StringBuilder sb = new StringBuilder();
        String loopDelimit = "";
        for (ObjectAttributesProvider elem : objectAttributesProviders) {
            String tmp = elem.getAttribute(o);
            if ((tmp != null) && !tmp.isEmpty()) {
                sb.append(loopDelimit);
                sb.append(tmp);
                loopDelimit = ",";
            }
        }
        return sb.toString();
    }

    /**
     * Set the DOT attributes for a specific field. This allows you to
     * change the appearance of certain edges in the output, but requires
     * that you know something about dot attributes.  Simple attributes
     * are, e.g., "color=blue".
     *
     * @param field  field to set attributes to
     * @param attrib field attributes
     * @return this
     */
    public LJV addFieldAttribute(Field field, String attrib) {
        this.fieldAttributesProviders.add(new FixedFieldAttributesProvider(field, attrib));
        return this;
    }

    public LJV addFieldAttributesProvider(FieldAttributesProvider provider) {
        this.fieldAttributesProviders.add(provider);
        return this;
    }

    public String getFieldAttributes(Field field, Object value) {
        StringBuilder sb = new StringBuilder();
        String loopDelimit = "";
        for (FieldAttributesProvider elem : fieldAttributesProviders) {
            String tmp = elem.getAttribute(field, value);
            if ((tmp != null) && !tmp.isEmpty()) {
                sb.append(loopDelimit);
                sb.append(tmp);
                loopDelimit = ",";
            }
        }
        return sb.toString();
    }

    /**
     * Set the DOT attributes for all fields with this name.
     *
     * @param field  field name to set attributes to
     * @param attrib attributes
     * @return current ljv object
     */
    public LJV addFieldAttribute(String field, String attrib) {
        this.fieldAttributesProviders.add(new FixedFieldNameAttributesProvider(field, attrib));
        return this;
    }

    /**
     * Do not display this field.
     *
     * @param field field to ignore
     * @return this
     */
    public LJV addIgnoreField(Field field) {
        this.ignoreSet.add(field);
        return this;
    }

    /**
     * Do not display any fields with this name.
     *
     * @param field field name to ignore
     * @return this
     */
    public LJV addIgnoreField(String field) {
        this.ignoreSet.add(field);
        return this;
    }

    /**
     * Do not display any fields from this class.
     *
     * @param cz class to ignore fields
     * @return this
     */
    public LJV addIgnoreFields(Class<?> cz) {
        Field[] fs = cz.getDeclaredFields();
        for (Field f : fs) this.addIgnoreField(f);
        return this;
    }

    /**
     * Do not display any fields with this type.
     *
     * @param cz type of fields to ignore
     * @return this
     */
    public LJV addIgnoreClass(Class<?> cz) {
        this.ignoreSet.add(cz);
        return this;
    }

    /**
     * Do not display any fields that have a type from this package.
     *
     * @param pk package that contains classes of fields that will not be displayed
     * @return this
     */
    public LJV addIgnorePackage(Package pk) {
        this.ignoreSet.add(pk);
        return this;
    }

    public boolean canIgnoreField(Field field) {
        return
                Modifier.isStatic(field.getModifiers())
                        || ignoreSet.contains(field)
                        || ignoreSet.contains(field.getName())
                        || ignoreSet.contains(field.getType())
                        || ignoreSet.contains(field.getType().getPackage())
                ;
    }

    public LJV addArrayElementAttributeProvider(ArrayElementAttributeProvider provider) {
        arrayElementAttributeProviders.add(Objects.requireNonNull(provider));
        return this;
    }

    /**
     * Enable highlighting array elements that was changed since previous run of ljv.
     *
     * @return current ljv object
     */
    public LJV highlightChangingArrayElements() {
        addArrayElementAttributeProvider(new ChangingArrayElementHighlighter());
        return this;
    }

    /**
     * Enable highlighting of new objects that appeared since previous run of ljv.
     *
     * @return current ljv object
     */
    public LJV highlightNewObjects() {
        addObjectAttributesProvider(new NewObjectHighlighter());
        return this;
    }

    public String getArrayElementAttributes(Object array, int index) {
        StringBuilder sb = new StringBuilder();
        String loopDelimit = "";
        for (ArrayElementAttributeProvider elem : arrayElementAttributeProviders) {
            String tmp = elem.getAttribute(array, index);
            if ((tmp != null) && !tmp.isEmpty()) {
                sb.append(loopDelimit);
                sb.append(tmp);
                loopDelimit = " ";
            }
        }
        String result = sb.toString();
        if (!result.isEmpty()) {
            return " " + result;
        } else {
            return "";
        }
    }

    List<Object> getRoots() {
        return roots;
    }

    /**
     * Treat objects of this class as primitives; i.e., {@code toString}
     * is called on the object, and the result displayed in the label like
     * a primitive field.
     *
     * @param cz Class of objects to be treated as primitives
     * @return this
     */
    public LJV setTreatAsPrimitive(Class<?> cz) {
        this.pretendPrimitiveSet.add(cz);
        return this;
    }

    public boolean isTreatsAsPrimitive(Class<?> cz) {
        return pretendPrimitiveSet.contains(cz);
    }

    /**
     * Treat objects from this package as primitives; i.e.,
     * {@code toString} is called on the object, and the result displayed
     * in the label like a primitive field.
     *
     * @param pk Package with classes to treated as primitive
     * @return this
     */
    public LJV setTreatAsPrimitive(Package pk) {
        this.pretendPrimitiveSet.add(pk);
        return this;
    }

    public boolean isTreatsAsPrimitive(Package pk) {
        return pretendPrimitiveSet.contains(pk);
    }

    private void setOption(boolean flag, Options option) {
        if (flag) {
            oSet.add(option);
        } else {
            oSet.remove(option);
        }
    }

    public LJV setIgnorePrivateFields(boolean ignorePrivateFields) {
        setOption(ignorePrivateFields, Options.IGNOREPRIVATEFIELDS);
        return this;
    }

    public boolean isIgnorePrivateFields() {
        return oSet.contains(Options.IGNOREPRIVATEFIELDS);
    }

    public LJV setShowFieldNamesInLabels(boolean showFieldNamesInLabels) {
        setOption(showFieldNamesInLabels, Options.SHOWFIELDNAMESINLABELS);
        return this;
    }

    public boolean isShowFieldNamesInLabels() {
        return oSet.contains(Options.SHOWFIELDNAMESINLABELS);
    }

    public LJV setQualifyNestedClassNames(boolean qualifyNestedClassNames) {
        setOption(qualifyNestedClassNames, Options.QUALIFYNESTEDCLASSNAMES);
        return this;
    }

    public boolean isQualifyNestedClassNames() {
        return oSet.contains(Options.QUALIFYNESTEDCLASSNAMES);
    }


    public LJV setShowPackageNamesInClasses(boolean showPackageNamesInClasses) {
        setOption(showPackageNamesInClasses, Options.SHOWPACKAGENAMESINCLASSES);
        return this;
    }

    public boolean isShowPackageNamesInClasses() {
        return oSet.contains(Options.SHOWPACKAGENAMESINCLASSES);
    }

    /**
     * Toggle whether to ignore fields with null values.
     *
     * @param ignoreNullValuedFields {@code true}, if we want to hide the fields with null values.
     * @return this
     */
    public LJV setIgnoreNullValuedFields(boolean ignoreNullValuedFields) {
        setOption(ignoreNullValuedFields, Options.IGNORENULLVALUEDFIELDS);
        return this;
    }

    public boolean isIgnoreNullValuedFields() {
        return oSet.contains(Options.IGNORENULLVALUEDFIELDS);
    }


    /**
     * add an Object to {@code roots}
     *
     * @param root New root object to visialize
     * @return this
     */
    public LJV addRoot(Object root) {
        this.roots.add(root);
        return this;
    }

    /**
     * Create a graph of the object rooted at {@code obj}.
     *
     * @param obj object to be visualized
     * @return String representation containing DOT commands to build the graph
     */
    public String drawGraph(Object obj) {
        addRoot(obj);
        return drawGraph();
    }

    /**
     * roots {@code roots} references counts can be visualized
     *
     * @return String representation containing DOT commands to build the graph
     */
    public String drawGraph() {
        Visualization visualizer = new GraphvizVisualization(this);
        visualizer.diagramBegin();

        for (Object obj : getRoots()) {
            if (visualizer.alreadyVisualized(obj)) continue;
            Node root = parseGraph(obj);
            root.visit(visualizer);
        }

        return visualizer.diagramEnd();
    }

    private Node parseGraph(Object obj) {
        Introspection introspection = new IntrospectionWithReflectionAPI(this);
        return introspection.parseGraph(obj, introspection.getObjClassName(obj, false), false, null);
    }
}
