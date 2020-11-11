package org.atpfivt.ljv;

//- Author:     John Hamer <J.Hamer@cs.auckland.ac.nz>
//- Created:    Sat May 10 15:27:48 2003
//- Time-stamp: <2004-08-23 12:47:06 jham005>

//- Copyright (C) 2004  John Hamer, University of Auckland
//-
//-   This program is free software; you can redistribute it and/or
//-   modify it under the terms of the GNU General Public License
//-   as published by the Free Software Foundation; either version 2
//-   of the License, or (at your option) any later version.
//-   
//-   This program is distributed in the hope that it will be useful,
//-   but WITHOUT ANY WARRANTY; without even the implied warranty of
//-   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//-   GNU General Public License for more details.
//-   
//-   You should have received a copy of the GNU General Public License along
//-   with this program; if not, write to the Free Software Foundation, Inc.,
//-   59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.

import java.lang.reflect.*;
import java.util.*;

/**
 * Lightweight Java Visualizer.
 */
public final class LJV {
    private final Map<Object, String> classAttributeMap = new HashMap<>();
    private final Map<Object, String> fieldAttributeMap = new HashMap<>();
    private final Set<Object> pretendPrimitiveSet = new HashSet<>();
    private final Set<Object> ignoreSet = new HashSet<>();
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
     */
    public void setClassAttribute(Class<?> cz, String attrib) {
        classAttributeMap.put(cz, attrib);
    }

    public String getClassAttribute(Class<?> cz) {
        return classAttributeMap.get(cz);
    }

    public LJV addClassAttribute(Class<?> cz, String attrib) {
        this.setClassAttribute(cz, attrib);
        return this;
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
        this.fieldAttributeMap.put(field, attrib);
        return this;
    }

    public String getFieldAttribute(Field field) {
        return fieldAttributeMap.get(field);
    }

    /**
     * Set the DOT attributes for all fields with this name.
     *
     * @param field field name to set attributes to
     * @return attributes
     */
    public String getFieldAttribute(String field) {
        return fieldAttributeMap.get(field);
    }

    public LJV addFieldAttribute(String field, String attrib) {
        this.fieldAttributeMap.put(field, attrib);
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

    /**
     * Treat objects of this class as primitives; i.e., <code>toString</code>
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
     * <code>toString</code> is called on the object, and the result displayed
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
     * Create a graph of the object rooted at @code{obj}.
     *
     * @param obj object to be visualized
     * @return String representation containing DOT commands to build the graph
     */
    public String drawGraph(Object obj) {
        return new GraphBuilder(this).generateDOT(obj);
    }
}
