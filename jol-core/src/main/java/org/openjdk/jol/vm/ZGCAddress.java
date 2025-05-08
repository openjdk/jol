package org.openjdk.jol.vm;

/**
 * Utility class for normalizing ZGC addresses by removing color bits
 *
 * @see <a href="https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/z/zAddress.hpp">Layout description</a>
 */
class ZGCAddress {
    private static final long REMAPPED_BITS_MASK = 0b1111L << 12;
    private static final long CLEAR_UNUSED_BITS_MASK = (1L << 46) - 1;
    private static final long COLOR_BITS_COUNT = 16;
    private static final boolean isAarch = "aarch64".equals(System.getProperty("os.arch"));

    static long uncolorize(long address) {
        return isAarch ? uncolorizeAarch(address) : uncolorizeNonAarch(address);
    }

    private static long uncolorizeNonAarch(long address) {
        int shift = Long.numberOfTrailingZeros(address & REMAPPED_BITS_MASK) + 1;
        return (address >> shift) & CLEAR_UNUSED_BITS_MASK;
    }

    private static long uncolorizeAarch(long address) {
        return (address >> COLOR_BITS_COUNT) & CLEAR_UNUSED_BITS_MASK;
    }

    private ZGCAddress() {}
}
