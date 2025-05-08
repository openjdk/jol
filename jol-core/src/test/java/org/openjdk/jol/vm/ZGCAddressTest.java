package org.openjdk.jol.vm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ZGCAddressTest {
    @Test
    public void testGenerationalZGCAddressUncolorize() {
        final long[] coloredAddresses;
        if ("aarch64".equals(System.getProperty("os.arch"))) {
            coloredAddresses = new long[] {
                // remapped bits inverted, no address and color overlap
                0b00100100_00000000_00000000_00111011_10100110_10111000_11100101_00010000L,
                0b00100100_00000000_00000000_00111011_10100110_10111000_11010101_00010000L,
                0b00100100_00000000_00000000_00111011_10100110_10111000_10110101_00010000L,
                0b00100100_00000000_00000000_00111011_10100110_10111000_01110101_00010000L
            };
        } else {
            coloredAddresses = new long[] {
                0b00100100_00000000_00000000_00111011_10100110_10111000_10000101_00010000L,
                0b00010010_00000000_00000000_00011101_11010011_01011100_01000101_00010000L,
                0b00001001_00000000_00000000_00001110_11101001_10101110_00100101_00010000L,
                0b00000100_10000000_00000000_00000111_01110100_11010111_00010101_00010000L,
            };
        }

        for (long address : coloredAddresses) {
            assertEquals(
                    0b100100_00000000_00000000_00111011_10100110_10111000L,
                    ZGCAddress.uncolorize(address)
            );
        }
    }
}