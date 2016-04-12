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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.openjdk.jol.util.IOUtils;
import sun.management.VMManagement;

import static org.openjdk.jol.vm.sa.Constants.*;

/**
 * Hotspot Serviceability Agent support.
 *
 * <pre>
 * <b>IMPORTANT NOTE:</b>
 *      On some UNIX based operating systems and MacOSX operation system, Hotspot Serviceability Agent (SA) process
 *      attach may fail due to insufficient privilege. So, on these operating systems, user (runs the application)
 *      must be super user and must be already authenticated for example with <code>"sudo"</code>
 *      command (also with password) to <code>"/etc/sudoers"</code> file.
 *
 *      For more information about <code>"sudo"</code>, please have a look:
 *          <a href="http://en.wikipedia.org/wiki/Sudo">http://en.wikipedia.org/wiki/Sudo</a>
 *          <a href="http://linux.about.com/od/commands/l/blcmdl8_sudo.htm">http://linux.about.com/od/commands/l/blcmdl8_sudo.htm</a>
 * </pre>
 *
 * @see Task
 * @see Result
 *
 * @see UniverseData
 *
 * @author Serkan Ozal
 */
public class ServiceabilityAgentSupport {

    private static ServiceabilityAgentSupport INSTANCE;

    public static ServiceabilityAgentSupport instance() {
        if (INSTANCE != null) return INSTANCE;
        if (Boolean.getBoolean(SKIP_HOTSPOT_SA_ATTACH_FLAG)) {
            throw new IllegalStateException("HotSpot Serviceability Agent attach skipped due to " + SKIP_HOTSPOT_SA_ATTACH_FLAG + " flag.");
        }
        INSTANCE = new ServiceabilityAgentSupport();
        return INSTANCE;
    }

    private final int processId;
    private final String classpathForAgent;
    private final boolean sudoRequired;

    private ServiceabilityAgentSupport() {
        processId = getCurrentProcId();
        classpathForAgent = getClassPath();
        sudoRequired = needSudo();
    }

    private boolean needSudo() {
        try {
            // First check attempt for HotSpot agent connection without "sudo" command
            callAgent(null, false);
            return false;
        } catch (ProcessAttachFailedException e1) {
            // Possibly because of insufficient privilege. So "sudo" is required.
            // So if "sudo" command is valid on OS and user allows "sudo" usage
            if (isSudoValidOS() && Boolean.getBoolean(TRY_WITH_SUDO_FLAG)) {
                try {
                    // Second check attempt for HotSpot agent connection but this time with "sudo" command
                    callAgent(null, true);
                    return true;
                } catch (Throwable t) {
                    throw new SASupportException("Unable to attach even with super-user privileges: " + t.getMessage(), t);
                }
            } else {
                throw new SASupportException("You can try again with super-user privileges. Use -D" + TRY_WITH_SUDO_FLAG + "=true to try with sudo.");
            }
        } catch (Throwable t) {
            throw new SASupportException(t.getMessage(), t);
        }
    }

    private static String getClassPath() {
        final String currentClasspath = normalizePath(ManagementFactory.getRuntimeMXBean().getClassPath());
        try {
            // Search it at classpath
            Class.forName(HOTSPOT_AGENT_CLASSNAME);

            // Use current classpath for agent process
            return currentClasspath;
        } catch (ClassNotFoundException e) {
            try {
                // If it couldn't be found at classpath, try to find it at
                File hotspotAgentLib = new File(normalizePath(System.getProperty("java.home")) + "/../lib/sa-jdi.jar");
                if (hotspotAgentLib.exists()) {
                    return currentClasspath + File.pathSeparator + normalizePath(hotspotAgentLib.getAbsolutePath());
                } else {
                    throw new SASupportException("Couldn't find HotSpot Serviceability Agent library (sa-jdi.jar).");
                }
            } catch (Throwable t) {
                throw new SASupportException("Couldn't find HotSpot Serviceability Agent library (sa-jdi.jar).", t);
            }
        }
    }

    private static String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    private static boolean isSudoValidOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("nix") || osName.contains("nux") || osName.contains("aix") || osName.contains("mac");
    }

    private static int getCurrentProcId() {
        try {
            // Find current process id to connect via HotSpot agent
            RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();

            Field jvmField = mxbean.getClass().getDeclaredField("jvm");
            jvmField.setAccessible(true);

            VMManagement management = (VMManagement) jvmField.get(mxbean);
            Method method = management.getClass().getDeclaredMethod("getProcessId");
            method.setAccessible(true);

            return (Integer) method.invoke(management);
        } catch (Throwable t) {
            throw new SASupportException("Couldn't find PID of current JVM process.", t);
        }
    }

    private Result callAgent(Task processor) {
        return callAgent(processor, sudoRequired);
    }

    private Result callAgent(Task processor, boolean sudoRequired) {
        // Generate required arguments to create an external Java process
        List<String> args = new ArrayList<String>();
        if (sudoRequired) {
            args.add("sudo");
        }
        args.add(normalizePath(System.getProperty("java.home")) + "/" + "bin" + "/" + "java");
        args.add("-cp");
        args.add(classpathForAgent);
        args.add(AttachMain.class.getName());

        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        BufferedReader err = null;
        Process agentProcess = null;
        try {
            // Create an external Java process to connect this process as HotSpot agent
            agentProcess = new ProcessBuilder(args).start();

            Request request = new Request(processId, processor, DEFAULT_TIMEOUT_IN_MSECS);

            // Get input, output and error streams
            InputStream is = agentProcess.getInputStream();
            OutputStream os = agentProcess.getOutputStream();
            InputStream es = agentProcess.getErrorStream();

            // Send request HotSpot agent process to execute
            out = new ObjectOutputStream(os);
            out.writeObject(request);
            out.flush();

            // At least, for all cases, wait process to finish
            int exitCode = agentProcess.waitFor();
            // Reset it, it has terminated and no need to destroy at the finally block
            agentProcess = null;

            // If process attach failed,
            if (exitCode == PROCESS_ATTACH_FAILED_EXIT_CODE) {
                throw new ProcessAttachFailedException("Attaching as HotSpot SA to current process " +
                                                       "(id=" + processId + ") from external process failed");
            }

            // At first, check errors
            err = new BufferedReader(new InputStreamReader(es));

            StringBuilder errBuilder = null;
            for (String line = err.readLine(); line != null; line = err.readLine()) {
                if (errBuilder == null) {
                    errBuilder = new StringBuilder();
                }
                errBuilder.append(line).append("\n");
            }
            if (errBuilder != null) {
                throw new RuntimeException(errBuilder.toString());
            }

            in = new ObjectInputStream(is);
            // Get response from HotSpot agent process
            Response response = (Response) in.readObject();

            if (response != null) {
                if (response.getError() != null) {
                    Throwable error = response.getError();
                    throw new RuntimeException(error.getMessage(), error);
                }

                return response.getResult();
            } else {
                return null;
            }
        } catch (ProcessAttachFailedException e) {
            throw e;
        } catch (Throwable t) {
            throw new SASupportException(t.getMessage(), t);
        } finally {
            IOUtils.safelyClose(out);
            IOUtils.safelyClose(in);
            IOUtils.safelyClose(err);

            if (agentProcess != null) {
                // If process is still in use, destroy it.
                // When it has terminated, it is set to "null" after "waitFor".
                agentProcess.destroy();
            }
        }
    }

    public UniverseData getUniverseData() {
        return (UniverseData) callAgent(new UniverseTask());
    }

}
