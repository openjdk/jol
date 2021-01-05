package org.atpfivt.ljv.provider.impl;

import org.atpfivt.ljv.provider.ObjectAttributesProvider;

import java.util.Objects;

public class FixedValueClassAttributes implements ObjectAttributesProvider {

    private final Class<?> aClass;
    private final String attributes;

    public FixedValueClassAttributes(Class<?> aClass, String values) {
        this.aClass = Objects.requireNonNull(aClass);
        this.attributes = Objects.requireNonNull(values);
    }

    @Override
    public String getAttribute(Object o) {
        if (aClass.equals(o.getClass())) {
            return attributes;
        } else {
            return "";
        }
    }
}
