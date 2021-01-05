package org.atpfivt.ljv.provider.impl;

import org.atpfivt.ljv.provider.ObjectAttributesProvider;

import java.util.IdentityHashMap;
import java.util.Map;

public class NewObjectHighlighter implements ObjectAttributesProvider {
    public static final String HIGHLIGHT = "style=filled,fillcolor=yellow";

    private final Map<Object, Object> known = new IdentityHashMap<>();
    private final Object dummy = new Object();

    @Override
    public String getAttribute(Object o) {
        if (known.put(o, dummy) == null) {
            return HIGHLIGHT;
        } else {
            return "";
        }
    }
}
