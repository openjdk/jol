package org.atpfivt.ljv;

import java.lang.reflect.Field;

/**
 * Provides DOT attributes (color, font etc.) for a given field.
 */
public interface FieldAttributesProvider {
    /**
     * Get map of attributes.
     * @param field Field information
     * @param val Field value
     * @return Map of attribute values
     */
    String getAttribute(Field field, Object val);

}
