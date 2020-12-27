package org.atpfivt.ljv;

import java.lang.reflect.Field;
import java.util.Objects;

public class FixedFieldNameAttributesProvider implements FieldAttributesProvider {
    private final String fieldName;
    private final String attributes;

    public FixedFieldNameAttributesProvider(String fieldName, String attributes) {
        this.fieldName = Objects.requireNonNull(fieldName);
        this.attributes = Objects.requireNonNull(attributes);
    }

    @Override
    public String getAttribute(Field field, Object val) {
        if (this.fieldName.equals(field.getName())) {
            return attributes;
        } else {
            return "";
        }
    }
}
