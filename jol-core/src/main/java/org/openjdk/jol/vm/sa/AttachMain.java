/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jol.vm.sa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.openjdk.jol.vm.sa.Constants.*;

class AttachMain {

    public static void main(final String[] args) {
        Response response = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        Object hotspotAgent = null;
        Method detachMethod = null;

        try {
            // Gets request from caller process over standard input
            in = new ObjectInputStream(System.in);
            out = new ObjectOutputStream(bos);

            System.setProperty("sun.jvm.hotspot.debugger.useProcDebugger", "true");
            System.setProperty("sun.jvm.hotspot.debugger.useWindbgDebugger", "true");

            final Request request = (Request) in.readObject();

            final Class<?> hotspotAgentClass = Class.forName(HOTSPOT_AGENT_CLASSNAME);
            hotspotAgent = hotspotAgentClass.newInstance();
            final Method attachMethod = hotspotAgentClass.getMethod("attach",int.class);
            detachMethod = hotspotAgentClass.getMethod("detach");

            Object vm = null;

            final Object agent = hotspotAgent;
            Thread t = new Thread() {
                public void run() {
                    try {
                        // Attach to the caller process as Hotspot agent
                        attachMethod.invoke(agent, request.getProcessId());
                    } catch (Throwable t) {
                        System.exit(PROCESS_ATTACH_FAILED_EXIT_CODE);
                    }
                };
            };
            t.start();

            // Check until timeout
            for (int i = 0; i < request.getTimeout(); i += VM_CHECK_PERIOD_SENSITIVITY_IN_MSECS) {
                try {
                    if ((vm = Class.forName(VM_CLASSNAME).getMethod("getVM").invoke(null)) != null) {
                        break;
                    }
                } catch (Throwable err) {
                    // There is nothing to do, try another
                }
                Thread.sleep(VM_CHECK_PERIOD_SENSITIVITY_IN_MSECS); // Wait a little before an attempt
            }

            // Check about if VM is initialized and ready to use
            if (vm != null) {
                final Task processor = request.getProcessor();
                if (processor != null) {
                    // Execute processor and gets its result
                    final Result result = processor.process();
                    response = new Response(result);
                }
            } else {
                throw new IllegalStateException("VM couldn't be initialized !");
            }
        } catch (Throwable t) {
            // If there is an error, attach it to response
            response = new Response(t);
        } finally {
            if (out != null) {
                try {
                    // Send response back to caller process over standard output
                    out.writeObject(response);
                    out.flush();
                    System.out.write(bos.toByteArray());
                } catch (IOException e) {
                    // There is nothing to do, so just ignore
                }
            }
            if (hotspotAgent != null && detachMethod != null) {
                try {
                    detachMethod.invoke(hotspotAgent);
                } catch (IllegalArgumentException e) {
                    // There is nothing to do, so just ignore
                } catch (IllegalAccessException e) {
                    // There is nothing to do, so just ignore
                } catch (InvocationTargetException e) {
                    // There is nothing to do, so just ignore
                }
            }
        }
    }
}
