package org.atpfivt.ljv.provider.impl;

import org.atpfivt.ljv.provider.FieldAttributesProvider;

import java.lang.reflect.Field;
import java.util.Objects;

public class FixedFieldAttributesProvider implements FieldAttributesProvider {
    private final Field field;
    private final String attributes;

    public FixedFieldAttributesProvider(Field field, String attributes) {
        this.field = Objects.requireNonNull(field);
        this.attributes = Objects.requireNonNull(attributes);
    }

    @Override
    public String getAttribute(Field field, Object val) {
        if (this.field.equals(field)) {
            return attributes;
        } else {
            return "";
        }
    }
}
