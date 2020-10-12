package ljv;

import java.lang.reflect.*;

final class ObjSettings {
    private final LJV ljv;
    public ObjSettings(LJV ljv) {
        this.ljv = ljv;
    }

    private boolean fieldExistsAndIsPrimitive(Field field, Object obj) {
        if (!ljv.canIgnoreField(field)) {
            try {
                //- The order of these statements matters.  If field is not
                //- accessible, we want an IllegalAccessException to be raised
                //- (and caught).  It is not correct to return true if
                //- field.getType( ).isPrimitive( )
                Object val = field.get(obj);
                if (field.getType().isPrimitive() || canTreatAsPrimitive(val))
                    //- Just calling ljv.canTreatAsPrimitive is not adequate --
                    //- val will be wrapped as a Boolean or Character, etc. if we
                    //- are dealing with a truly primitive type.
                    return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public boolean hasPrimitiveFields(Field[] fs, Object obj) {
        for (Field f : fs)
            if (fieldExistsAndIsPrimitive(f, obj))
                return true;
        return false;
    }

    private static boolean redefinesToString(Object obj) {
        Method[] ms = obj.getClass().getMethods();
        for (Method m : ms)
            if (m.getName().equals("toString") && m.getDeclaringClass() != Object.class)
                return true;
        return false;
    }


    public String className(Object obj, boolean useToStringAsClassName) {
        if (obj == null)
            return "";

        Class<?> c = obj.getClass();
        if (useToStringAsClassName && redefinesToString(obj))
            return Quote.quote(obj.toString());
        else {
            String name = c.getName();
            if (!ljv.isShowPackageNamesInClasses() || c.getPackage() == ljv.getClass().getPackage()) {
                //- Strip away everything before the last .
                name = name.substring(name.lastIndexOf('.') + 1);

                if (!ljv.isQualifyNestedClassNames())
                    name = name.substring(name.lastIndexOf('$') + 1);
            }
            return name;
        }
    }

    public boolean canTreatAsPrimitive(Object obj) {
        return obj == null || canTreatClassAsPrimitive(obj.getClass());
    }


    public boolean canTreatClassAsPrimitive(Class<?> cz) {
        if (cz == null || cz.isPrimitive())
            return true;

        if (cz.isArray())
            return false;

        do {
            if (ljv.isTreatsAsPrimitive(cz)
                    || ljv.isTreatsAsPrimitive(cz.getPackage())
            )
                return true;

            if (cz == Object.class)
                return false;

            Class<?>[] ifs = cz.getInterfaces();
            for (Class<?> anIf : ifs)
                if (canTreatClassAsPrimitive(anIf))
                    return true;

            cz = cz.getSuperclass();
        } while (cz != null);
        return false;
    }

    public boolean looksLikePrimitiveArray(Object obj) {
        Class<?> c = obj.getClass();
        if (c.getComponentType().isPrimitive())
            return true;

        for (int i = 0, len = Array.getLength(obj); i < len; i++)
            if (!canTreatAsPrimitive(Array.get(obj, i)))
                return false;
        return true;
    }
}
