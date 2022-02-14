package org.atpfivt.ljv;

/**
 * Represents direction of graph layout. Corresponds to possible values of rankdir command in Graphviz.
 */
public enum Direction {
    /**
     * draws from bottom to top
     */
    BT,
    /**
     * draws from left to right
     */
    LR,
    /**
     * draws from top to bottom (the default value)
     */
    TB,
    /**
     * draws from right to left
     */
    RL
}
