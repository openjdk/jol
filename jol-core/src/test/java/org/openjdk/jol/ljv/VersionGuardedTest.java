package org.openjdk.jol.ljv;

public class VersionGuardedTest {
    int VERSION = getVersion();

    public boolean is11() {
        return VERSION == 11;
    }

    static int getVersion() {
//        Java 8 or lower: 1.6.0_23, 1.7.0, 1.7.0_80, 1.8.0_211
//        Java 9 or higher: 9.0.1, 11.0.4, 12, 12.0.1
        String version = "-1";
        String fullVersion = System.getProperty("java.version");
        if (fullVersion.startsWith("1.")) {
            version = fullVersion.substring(2, 3);
        } else {
            int dotPos = fullVersion.indexOf(".");
            if (dotPos != -1) {
                version = fullVersion.substring(0, dotPos);
            }
        }
        return Integer.parseInt(version);
    }
}
