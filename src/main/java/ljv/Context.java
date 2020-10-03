package ljv;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class Context {
    private final Map<Object, String> classAttributeMap = new HashMap<>();
    private final Map<Object, String> fieldAttributeMap = new HashMap<>();
    private final Set<Object> pretendPrimitiveSet = new HashSet<>();
    private final Set<Object> ignoreSet = new HashSet<>();

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
    private final EnumSet<Options> oSet = EnumSet.of(Options.SHOWPACKAGENAMESINCLASSES, Options.SHOWFIELDNAMESINLABELS);

    /**
     * Set the DOT attributes for a class.  This allows you to change the
     * appearance of certain nodes in the output, but requires that you
     * know something about dot attributes.  Simple attributes are, e.g.,
     * "color=red".
     */
    public void setClassAttribute(Class<?> cz, String attrib) {
        classAttributeMap.put(cz, attrib);
    }

    public String getClassAtribute(Class<?> cz) {
        return classAttributeMap.get(cz);
    }

    public Context addClassAttribute(Class<?> cz, String attrib) {
        this.setClassAttribute(cz, attrib);
        return this;
    }

    /**
     * Set the DOT attributes for a specific field. This allows you to
     * change the appearance of certain edges in the output, but requires
     * that you know something about dot attributes.  Simple attributes
     * are, e.g., "color=blue".
     */
    public Context addFieldAttribute(Field field, String attrib) {
        this.fieldAttributeMap.put(field, attrib);
        return this;
    }

    public String getFieldAttribute(Field field) {
        return fieldAttributeMap.get(field);
    }

    /**
     * Set the DOT attributes for all fields with this name.
     */
    public String getFieldAttribute(String field) {
        return fieldAttributeMap.get(field);
    }

    public Context addFieldAttribute(String field, String attrib) {
        this.fieldAttributeMap.put(field, attrib);
        return this;
    }

    /**
     * Do not display this field.
     */
    public Context addIgnoreField(Field field) {
        this.ignoreSet.add(field);
        return this;
    }

    /**
     * Do not display any fields with this name.
     */
    public Context addIgnoreField(String field) {
        this.ignoreSet.add(field);
        return this;
    }

    /**
     * Do not display any fields from this class.
     */
    public Context addIgnoreFields(Class<?> cz) {
        Field[] fs = cz.getDeclaredFields();
        for (int i = 0; i < fs.length; i++)
            this.addIgnoreField(fs[i]);
        return this;
    }

    /**
     * Do not display any fields with this type.
     */
    public Context addIgnoreClass(Class<?> cz) {
        this.ignoreSet.add(cz);
        return this;
    }

    /**
     * Do not display any fields that have a type from this package.
     */
    public Context addIgnorePackage(Package pk) {
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
     */
    public Context setTreatAsPrimitive(Class<?> cz) {
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
     */
    public Context setTreatAsPrimitive(Package pk) {
        this.pretendPrimitiveSet.add(pk);
        return this;
    }

    public boolean isTreatsAsPrimitive(Package pk) {
        return pretendPrimitiveSet.contains(pk);
    }

    private void setOption(boolean flag, Options option) {
        if (flag) {
            oSet.add(option);
        }
        else {
            oSet.remove(option);
        }
    }

    public Context setIgnorePrivateFields(boolean ignorePrivateFields) {
        setOption(ignorePrivateFields, Options.IGNOREPRIVATEFIELDS);
        return this;
    }

    public boolean isIgnorePrivateFields() {
        return oSet.contains(Options.IGNOREPRIVATEFIELDS);
    }

    public Context setShowFieldNamesInLabels(boolean showFieldNamesInLabels) {
        setOption(showFieldNamesInLabels, Options.SHOWFIELDNAMESINLABELS);
        return this;
    }

    public boolean isShowFieldNamesInLabels() {
        return oSet.contains(Options.SHOWFIELDNAMESINLABELS);
    }

    public Context setQualifyNestedClassNames(boolean qualifyNestedClassNames) {
        setOption(qualifyNestedClassNames, Options.QUALIFYNESTEDCLASSNAMES);
        return this;
    }

    public boolean isQualifyNestedClassNames() {
        return oSet.contains(Options.QUALIFYNESTEDCLASSNAMES);
    }


    public Context setShowPackageNamesInClasses(boolean showPackageNamesInClasses) {
        setOption(showPackageNamesInClasses, Options.SHOWPACKAGENAMESINCLASSES);
        return this;
    }

    public boolean isShowPackageNamesInClasses() {
        return oSet.contains(Options.SHOWPACKAGENAMESINCLASSES);
    }
}
