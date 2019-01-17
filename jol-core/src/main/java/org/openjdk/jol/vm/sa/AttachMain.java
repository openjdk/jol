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

import org.openjdk.jol.util.ClassUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;

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

            final Class<?> hotspotAgentClass = ClassUtils.loadClass(HOTSPOT_AGENT_CLASSNAME);
            hotspotAgent = hotspotAgentClass.newInstance();
            final Method attachMethod = hotspotAgentClass.getMethod("attach",int.class);
            detachMethod = hotspotAgentClass.getMethod("detach");

            // Attach from a separate thread to capture timeouts. Do not block the
            // main thread waiting for the assert to happen.

            final Object agent = hotspotAgent;
            Future<?> future = Executors.newCachedThreadPool(new MyThreadFactory())
                    .submit(new Callable<Object>() {
                                @Override
                                public Object call() {
                                    try {
                                        // Attach to the caller process as Hotspot agent
                                        attachMethod.invoke(agent, (int) request.getProcessId());
                                        return ClassUtils.loadClass(VM_CLASSNAME).getMethod("getVM").invoke(null);
                                    } catch (Exception t) {
                                        throw new RuntimeException(t);
                                    }
                                }
                            }
                    );

            Object vm = future.get(request.getTimeout(), TimeUnit.MILLISECONDS);
            if (vm != null) {
                final Task processor = request.getProcessor();
                if (processor != null) {
                    // Execute processor and gets its result
                    final Result result = processor.process();
                    response = new Response(result);
                }
            } else {
                throw new IllegalStateException("VM couldn't be initialized!");
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
                } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                    // There is nothing to do, so just ignore
                }
            }
        }
    }

    static class MyThreadFactory implements ThreadFactory {
        final ThreadFactory f = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = f.newThread(r);
            t.setDaemon(true);
            return t;
        }
    }

}
