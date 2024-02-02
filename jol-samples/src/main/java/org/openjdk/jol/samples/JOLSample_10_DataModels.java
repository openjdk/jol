/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jol.samples;

import org.openjdk.jol.datamodel.*;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.CurrentLayouter;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;

/**
 * @author Aleksey Shipilev
 */
public class JOLSample_10_DataModels {

    /*
     * This example shows the differences between the data models.
     * First layout is the actual VM layout, everything else are
     * simulations.
     *
     * JDK 15 brought a new layouter, which may pack fields more densely.
     * JDK 15 also allows to have compressed class pointers without
     * compressed oops.
     *
     * You can see the reference sizes are different, depending on VM model:
     *  - with 32-bit model, reference size is 4 bytes
     *  - with 64-bit model, reference size is 8 bytes;
     *      unless compressed references are enabled
     *      (enabled by default when heap size is small)
     *
     * The mark word sizes depend on JVM bitness:
     *  - with 32-bit model, mark word size is 4 bytes
     *  - with 64-bit model, mark word size is 8 bytes
     *
     * The class word sizes depend on JVM model:
     *  - with 32-bit model, class word is 4 bytes
     *  - with 64-bit model, class word is 8 bytes;
     *     unless compressed class pointers are enabled
     *     (enabled by default when compressed references are enabled)
     *     (since JDK 15, can be enabled even without compressed references)
     */

    private static final DataModel[] MODELS_JDK8 = new DataModel[]{
            new Model32(),
            new Model64(false, false),
            new Model64(true, true),
            new Model64(true, true, 16),
    };

    private static final DataModel[] MODELS_JDK15 = new DataModel[]{
            new Model64(false, true),
            new Model64(false, true, 16),
    };

    public static void main(String[] args) {
        {
            Layouter l = new CurrentLayouter();
            System.out.println("***** " + l);
            System.out.println(ClassLayout.parseClass(A.class, l).toPrintable());
        }

        for (DataModel model : MODELS_JDK8) {
            Layouter l = new HotSpotLayouter(model, 8);
            System.out.println("***** " + l);
            System.out.println(ClassLayout.parseClass(A.class, l).toPrintable());
        }

        for (DataModel model : MODELS_JDK8) {
            Layouter l = new HotSpotLayouter(model, 15);
            System.out.println("***** " + l);
            System.out.println(ClassLayout.parseClass(A.class, l).toPrintable());
        }

        for (DataModel model : MODELS_JDK15) {
            Layouter l = new HotSpotLayouter(model, 15);
            System.out.println("***** " + l);
            System.out.println(ClassLayout.parseClass(A.class, l).toPrintable());
        }
    }

    public static class A {
        Object a;
        int b;
    }

}
