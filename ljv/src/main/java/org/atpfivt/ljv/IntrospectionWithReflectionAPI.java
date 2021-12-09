package org.atpfivt.ljv;

import org.atpfivt.ljv.jol.ClassLayout;
import org.atpfivt.ljv.jol.FieldLayout;
import org.atpfivt.ljv.nodes.*;
import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.util.ObjectUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IntrospectionWithReflectionAPI implements Introspection {
    private final IdentityHashMap<Object, ObjectNode> alreadyVisitedObjects = new IdentityHashMap<>();
    protected final LJV ljv;

    public IntrospectionWithReflectionAPI(LJV ljv) {
        this.ljv = ljv;
    }

    @Override
    public Node parseGraph(Object obj, String name, boolean isPrimitive, Field field) {
        if (isPrimitive) {
            return new PrimitiveNode(obj, name);
        }

        if (obj == null) {
            return new NullNode(null, name);
        }

        // Не зацикливаемся, смотрим обошли мы этот объект уже или ещё нет.
        ObjectNode oldNode = alreadyVisitedObjects.get(obj);
        if (oldNode != null) {
            ObjectNode objectNode = new ObjectNode(oldNode);
            objectNode.setName(name);
            if (field != null) {
                objectNode.setAttributes(ljv.getFieldAttributes(field, obj));
            }
            return objectNode;
        }

        if (obj.getClass().isArray()) {
            ArrayNode arrayNode = new ArrayNode(obj, name, catTreatObjAsArrayOfPrimitives(obj), getArrayContent(obj));
            if (field != null) {
                arrayNode.setAttributes(ljv.getFieldAttributes(field, obj));
            }
            return arrayNode;
        }

        ObjectNode objectNode = new ObjectNode(obj, name,
                getObjClassName(obj, false),
                countObjectPrimitiveFields(obj), null,
                "");
        alreadyVisitedObjects.put(obj, objectNode);
        if (field != null) {
            objectNode.setAttributes(ljv.getFieldAttributes(field, obj));
        }
        objectNode.setChildren(getChildren(obj));
        return objectNode;
    }

    @Override
    public List<Node> getChildren(Object obj) {
        List<Node> result = new ArrayList<>();

        Field[] fields = getObjFields(obj);

        for (Field field : fields) {
            if (!(Modifier.isStatic(field.getModifiers()))
                    && !ljv.canIgnoreField(field)
            ) {
                Node node = parseGraph(ObjectUtils.value(obj, field), field.getName(), objectFieldIsPrimitive(field, obj), field);
                if (node != null) result.add(node);
            }
        }
        return result;
    }

    private List<Node> getArrayContent(Object obj) {
        List<Node> result = new ArrayList<>();
        int len = Array.getLength(obj);
        for (int i = 0; i < len; i++) {
            Object ref = Array.get(obj, i);
            result.add(parseGraph(ref, getObjClassName(ref, false), false, null));
        }
        return result;
    }

    @Override
    public boolean canBeConvertedToString(Object obj) {
        Method[] ms = obj.getClass().getMethods();
        for (Method m : ms) {
            if (m.getName().equals("toString") && m.getDeclaringClass() != Object.class) {
                return true;
            }
        }
        return false;
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
    public int countObjectPrimitiveFields(Object obj) {
        int size = 0;
        Field[] fields = getObjFields(obj);
        for (Field field : fields) {
            if (objectFieldIsPrimitive(field, obj)) {
                size++;
            }
        }
        return size;
    }

    @Override
    public boolean hasPrimitiveFields(Object obj) {
        return countObjectPrimitiveFields(obj) > 0;
    }

    @Override
    public boolean objectFieldIsPrimitive(Field field, Object obj) {
        if (!ljv.canIgnoreField(field)) {
            //- The order of these statements matters. It is not correct
            //- to return true if field.getType( ).isPrimitive( )
            Object val = ObjectUtils.value(obj, field);
            //- Just calling ljv.canTreatAsPrimitive is not adequate --
            //- val will be wrapped as a Boolean or Character, etc. if we
            //- are dealing with a truly primitive type.
            return field.getType().isPrimitive() || canTreatObjAsPrimitive(val);
        }

        return false;
    }

    @Override
    public Field[] getObjFields(Object obj) {
        Class<?> cls = obj.getClass();

        SortedSet<FieldLayout> fieldLayouts = ClassLayout.parseClass(cls).fields();

        Stream<Field> fieldStream = fieldLayouts.stream()
                .map(FieldLayout::data).map(FieldData::refField).filter(getObjFieldsIgnoreNullValuedPredicate(obj));

        return fieldStream.toArray(Field[]::new);
    }

    private Predicate<Field> getObjFieldsIgnoreNullValuedPredicate(Object obj) {
        return (Field f) -> {
            if (ljv.isIgnoreNullValuedFields()) {
                return ObjectUtils.value(obj, f) != null;
            }
            return true;
        };
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

    @Override
    public boolean canTreatObjAsPrimitive(Object obj) {
        return obj == null || canTreatClassAsPrimitive(obj.getClass());
    }

    @Override
    public boolean catTreatObjAsArrayOfPrimitives(Object obj) {
        Class<?> c = obj.getClass();
        if (c.getComponentType().isPrimitive()) {
            return true;
        }

        for (int i = 0, len = Array.getLength(obj); i < len; i++) {
            if (!canTreatObjAsPrimitive(Array.get(obj, i))) {
                return false;
            }
        }

        return true;
    }

}
