package org.atpfivt.ljv.provider;

public enum NodeType {
    ARRAY,
    REFERENCE, //non-primitive object, if a value of a field, then an arrow should exist
    PRIMITIVE, //valid only for a field
    NULL_REFERENCE, //we draw it as a separate node, we do not deduplicate nulls
          //'null' value can be either PRIMITIVE, NULL or IGNORE
    IGNORE //do not show
}
