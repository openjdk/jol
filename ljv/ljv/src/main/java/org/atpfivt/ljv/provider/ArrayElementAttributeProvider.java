package org.atpfivt.ljv.provider;

@FunctionalInterface
public interface ArrayElementAttributeProvider {
    String getAttribute(Object array, int index);
}
