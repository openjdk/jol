package org.atpfivt.ljv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComparingArrayElementAttributeProviderTest {
    ComparingArrayElementAttributeProvider provider = new ComparingArrayElementAttributeProvider();

    @Test
    void checksChangedElements() {
        int[] arr = new int[]{1, 2, 3};
        for (int i = 0; i < arr.length; i++) {
            assertEquals("", provider.getAttribute(arr, i));
        }
        arr[0] = 2;
        arr[2] = 4;
        assertEquals("bgcolor=\"yellow\"", provider.getAttribute(arr, 0));
        assertEquals("", provider.getAttribute(arr, 1));
        assertEquals("bgcolor=\"yellow\"", provider.getAttribute(arr, 2));
    }
}