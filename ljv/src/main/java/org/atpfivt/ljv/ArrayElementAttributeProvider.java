package org.atpfivt.ljv;

@FunctionalInterface
public interface ArrayElementAttributeProvider {
    String getAttribute(Object array, int index);
}
