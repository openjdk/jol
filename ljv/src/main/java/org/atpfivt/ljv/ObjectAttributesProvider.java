package org.atpfivt.ljv;

/**
 * Provides DOT attributes (color, font etc.) for a given object.
 */
@FunctionalInterface
public interface ObjectAttributesProvider {
    /**
     * Get map of attributes.
     * @param o Object
     * @return Map of attribute values
     */
    String getAttribute(Object o);
}
