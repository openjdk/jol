package ljv;

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

class LJV {
    private final IdentityHashMap<Object, String> objectsId = new IdentityHashMap<>();

    private String dotName(Object obj) {
        return objectsId.computeIfAbsent(obj, s -> "n" + (objectsId.size() + 1));
    }


    private boolean fieldExistsAndIsPrimitive(Context ctx, Field field, Object obj) {
        if (!ctx.canIgnoreField(field)) {
            try {
                //- The order of these statements matters.  If field is not
                //- accessible, we want an IllegalAccessException to be raised
                //- (and caught).  It is not correct to return true if
                //- field.getType( ).isPrimitive( )
                Object val = field.get(obj);
                if (field.getType().isPrimitive() || canTreatAsPrimitive(ctx, val))
                    //- Just calling ctx.canTreatAsPrimitive is not adequate --
                    //- val will be wrapped as a Boolean or Character, etc. if we
                    //- are dealing with a truly primitive type.
                    return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private boolean hasPrimitiveFields(Context ctx, Field[] fs, Object obj) {
        for (int i = 0; i < fs.length; i++)
            if (fieldExistsAndIsPrimitive(ctx, fs[i], obj))
                return true;
        return false;
    }


    private void processPrimitiveArray(Object obj, StringBuilder out) {
        out.append(dotName(obj) + "[shape=record, label=\"");
        for (int i = 0, len = Array.getLength(obj); i < len; i++) {
            if (i != 0)
                out.append("|");
            out.append(Quote.quote(String.valueOf(Array.get(obj, i))));
        }
        out.append("\"];\n");
    }


    private void processObjectArray(Context ctx, Object obj, StringBuilder out) {
        out.append(dotName(obj) + "[label=\"");
        int len = Array.getLength(obj);
        for (int i = 0; i < len; i++) {
            if (i != 0)
                out.append("|");
            out.append("<f" + i + ">");
        }
        out.append("\",shape=record];\n");
        for (int i = 0; i < len; i++) {
            Object ref = Array.get(obj, i);
            if (ref == null)
                continue;
            generateDotInternal(ctx, ref, out);
            out.append(dotName(obj) + ":f" + i + " -> " + dotName(ref)
                    + "[label=\"" + i + "\",fontsize=12];\n");
        }
    }


    private void labelObjectWithSomePrimitiveFields(Context ctx, Object obj, Field[] fs, StringBuilder out) {
        Object cabs = ctx.getClassAtribute(obj.getClass());
        out.append(dotName(obj) + "[label=\"" + className(obj, ctx, false) + "|{");
        String sep = "";
        for (int i = 0; i < fs.length; i++) {
            Field field = fs[i];
            if (!ctx.canIgnoreField(field))
                try {
                    Object ref = field.get(obj);
                    if (field.getType().isPrimitive() || canTreatAsPrimitive(ctx, ref)) {
                        if (ctx.isShowFieldNamesInLabels())
                            out.append(sep + field.getName() + ": " + Quote.quote(String.valueOf(ref)));
                        else
                            out.append(sep + Quote.quote(String.valueOf(ref)));
                        sep = "|";
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
        }
        out.append("}\"" + (cabs == null ? "" : "," + cabs) + ",shape=record];\n");
    }


    private void labelObjectWithNoPrimitiveFields(Context ctx, Object obj, StringBuilder out) {
        Object cabs = ctx.getClassAtribute(obj.getClass());
        out.append(dotName(obj)
                + "[label=\"" + className(obj, ctx, true) + "\""
                + (cabs == null ? "" : "," + cabs)
                + "];\n");
    }

    private void processFields(Context ctx, Object obj, Field[] fs, StringBuilder out) {
        for (int i = 0; i < fs.length; i++) {
            Field field = fs[i];
            if (!ctx.canIgnoreField(field)) {
                try {
                    Object ref = field.get(obj);
                    if (field.getType().isPrimitive() || canTreatAsPrimitive(ctx, ref))
                        //- The field might be declared, say, Object, but the actual
                        //- object may be, say, a String.
                        continue;
                    String name = field.getName();
                    Object fabs = ctx.getFieldAttribute(field);
                    if (fabs == null)
                        fabs = ctx.getFieldAttribute(name);
                    generateDotInternal(ctx, ref, out);
                    out.append(dotName(obj) + " -> " + dotName(ref)
                            + "[label=\"" + name + "\",fontsize=12"
                            + (fabs == null ? "" : "," + fabs)
                            + "];\n");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean redefinesToString(Object obj) {
        Method[] ms = obj.getClass().getMethods();
        for (int i = 0; i < ms.length; i++)
            if (ms[i].getName().equals("toString") && ms[i].getDeclaringClass() != Object.class)
                return true;
        return false;
    }


    protected String className(Object obj, Context context, boolean useToStringAsClassName) {
        if (obj == null)
            return "";

        Class<?> c = obj.getClass();
        if (useToStringAsClassName && redefinesToString(obj))
            return Quote.quote(obj.toString());
        else {
            String name = c.getName();
            if (!context.isShowPackageNamesInClasses() || c.getPackage() == LJV.class.getPackage()) {
                //- Strip away everything before the last .
                name = name.substring(name.lastIndexOf('.') + 1);

                if (!context.isQualifyNestedClassNames())
                    name = name.substring(name.lastIndexOf('$') + 1);
            }
            return name;
        }
    }

    boolean canTreatAsPrimitive(Context context, Object obj) {
        return obj == null || canTreatClassAsPrimitive(context, obj.getClass());
    }


    private boolean canTreatClassAsPrimitive(Context context, Class<?> cz) {
        if (cz == null || cz.isPrimitive())
            return true;

        if (cz.isArray())
            return false;

        do {
            if (context.isTreatsAsPrimitive(cz)
                    || context.isTreatsAsPrimitive(cz.getPackage())
            )
                return true;

            if (cz == Object.class)
                return false;

            Class<?>[] ifs = cz.getInterfaces();
            for (int i = 0; i < ifs.length; i++)
                if (canTreatClassAsPrimitive(context, ifs[i]))
                    return true;

            cz = cz.getSuperclass();
        } while (cz != null);
        return false;
    }

    boolean looksLikePrimitiveArray(Object obj, Context context) {
        Class<?> c = obj.getClass();
        if (c.getComponentType().isPrimitive())
            return true;

        for (int i = 0, len = Array.getLength(obj); i < len; i++)
            if (!canTreatAsPrimitive(context, Array.get(obj, i)))
                return false;
        return true;
    }

    private void generateDotInternal(Context ctx, Object obj, StringBuilder out) {
        if (obj == null)
            out.append(dotName(obj) + "[label=\"null\"" + ", shape=plaintext];\n");
         else if (!objectsId.containsKey(obj)) {
            Class<?> c = obj.getClass();
            if (c.isArray()) {
                if (looksLikePrimitiveArray(obj, ctx))
                    processPrimitiveArray(obj, out);
                else
                    processObjectArray(ctx, obj, out);
            } else {
                Field[] fs = c.getDeclaredFields();
                if (!ctx.isIgnorePrivateFields())
                    AccessibleObject.setAccessible(fs, true);

                if (hasPrimitiveFields(ctx, fs, obj))
                    labelObjectWithSomePrimitiveFields(ctx, obj, fs, out);
                else
                    labelObjectWithNoPrimitiveFields(ctx, obj, out);

                processFields(ctx, obj, fs, out);
            }
        }
    }

    /**
     * Write a DOT digraph specification of the graph rooted at
     * <tt>obj</tt> to <tt>out</tt>.
     */
    private void generateDOT(Context ctx, Object obj, StringBuilder out) {
        out.append("digraph Java {\n");
        generateDotInternal(ctx, obj, out);
        out.append("}\n");
    }

    /**
     * Create a graph of the object rooted at <tt>obj</tt>.
     */
    public String drawGraph(Context ctx, Object obj) {
        StringBuilder out = new StringBuilder();
        generateDOT(ctx, obj, out);
        return out.toString();
    }

    public String drawGraph(Object obj) {
        return drawGraph(new Context(), obj);
    }
}
