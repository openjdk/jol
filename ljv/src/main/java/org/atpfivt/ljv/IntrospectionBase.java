package org.atpfivt.ljv;

public abstract class IntrospectionBase implements Introspection {
    protected final LJV ljv;

    public IntrospectionBase(LJV ljv) {
        this.ljv = ljv;
    }

    @Override
    public String getObjClassName(Object obj, boolean useToStringAsClassName) {
        if (obj == null)
            return "";

        Class<?> c = obj.getClass();
        if (useToStringAsClassName && canBeConvertedToString(obj))
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

    @Override
    public boolean canTreatObjAsPrimitive(Object obj) {
        return obj == null || canTreatClassAsPrimitive(obj.getClass());
    }

    @Override
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

}
