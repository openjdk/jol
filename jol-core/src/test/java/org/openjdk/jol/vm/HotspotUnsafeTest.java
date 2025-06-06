package org.openjdk.jol.vm;

import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.junit.Assert.assertEquals;

public class HotspotUnsafeTest {
    @Test
    public void testAlignment(){
        assertEquals(expectedAlignment(), VM.current().objectAlignment());
    }

    private int expectedAlignment() {
        Optional<String> alignmentProperty = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(arg -> arg.contains("-XX:ObjectAlignmentInBytes"))
                .findAny();
        return alignmentProperty
                .map(alignment -> Integer.parseInt(substringAfter(alignment, "=")))
                .orElse(8);
    }
}
