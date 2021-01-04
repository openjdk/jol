package org.atpfivt.ljv.provider.impl;

import org.atpfivt.ljv.provider.ArrayElementAttributeProvider;

import java.lang.reflect.Array;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public class ComparingArrayElementAttributeProvider implements ArrayElementAttributeProvider {
    Map<Object, Object> refCopy = new IdentityHashMap<>();

    Object cloneArray(Object arr) {
        int length = Array.getLength(arr);
        Class<?> componentType = arr.getClass().getComponentType();
        Object newArray = Array.newInstance(componentType, length);
        System.arraycopy(arr, 0, newArray, 0, length);
        return newArray;
    }

    @Override
    public String getAttribute(Object array, int index) {
        if (!array.getClass().isArray()) {
            throw new IllegalStateException();
        }
        Object copy = refCopy.computeIfAbsent(array, this::cloneArray);
        Object newValue = Array.get(array, index);
        if(!Objects.equals(newValue, Array.get(copy, index))){
            Array.set(copy, index, newValue);
            return "bgcolor=\"yellow\"";
        } else {
            return "";
        }
    }
}
