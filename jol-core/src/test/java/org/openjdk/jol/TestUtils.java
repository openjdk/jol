package org.openjdk.jol;

public class TestUtils {

    public static final int JDK_VERSION = getVersion();

    private static int getVersion() {
        try {
            return Integer.parseInt(System.getProperty("java.specification.version"));
        } catch (Exception e) {
            // Assume JDK 8
            return 8;
        }
    }

}
