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

import org.openjdk.jol.util.ClassUtils;
import org.openjdk.jol.util.IOUtils;

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

    private final long processId;
    private final boolean sudoRequired;
    private final AgentStyle agentStyle;

    private ServiceabilityAgentSupport() {
        processId = getCurrentProcId();
        agentStyle = senseAgentStyle();
        sudoRequired = needSudo(agentStyle);
    }

    private AgentStyle senseAgentStyle() {
        List<Throwable> exceptions = new ArrayList<>();
        for (AgentStyle style : AgentStyle.values()) {
            try {
                senseAccess(style);
                return style;
            } catch (Throwable t1) {
                // fall-through
                exceptions.add(t1);
            }
        }
        // TODO: Use addSuppressed once we are buildable with JDK 7
        throw new SASupportException("Unable to attach even with module exceptions: " + exceptions,
                exceptions.get(exceptions.size()-1));
    }

    private boolean needSudo(AgentStyle style) {
        try {
            callAgent(null, false, style);
            return false;
        } catch (Throwable t1) {
            if (isSudoValidOS() && Boolean.getBoolean(TRY_WITH_SUDO_FLAG)) {
                try {
                    callAgent(null, true, style);
                    return true;
                } catch (Throwable t2) {
                    throw new SASupportException("Unable to attach even with escalated privileges: " + t2.getMessage(), t2);
                }
            } else {
                throw new SASupportException("You can try again with escalated privileges. " +
                        "Two options: a) use -D" + TRY_WITH_SUDO_FLAG + "=true to try with sudo; b) echo 0 | sudo tee /proc/sys/kernel/yama/ptrace_scope");
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

    private static long getCurrentProcId() {
        // Try to use JDK 9 java.lang.ProcessHandle.current().getPid()
        try {
            Class<?> c = ClassUtils.loadClass("java.lang.ProcessHandle");
            Object current = c.getDeclaredMethod("current").invoke(null);
            return (Long) c.getDeclaredMethod("pid").invoke(current);
        } catch (Throwable t) {
            // okay, no support yet, falling through
        }

        // Find current process id to connect via RuntimeMXBean
        try {
            RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();

            Field jvmField = mxbean.getClass().getDeclaredField("jvm");
            jvmField.setAccessible(true);

            sun.management.VMManagement management = (sun.management.VMManagement) jvmField.get(mxbean);
            Method method = management.getClass().getDeclaredMethod("getProcessId");
            method.setAccessible(true);

            return (Integer) method.invoke(management);
        } catch (Throwable t) {
            throw new SASupportException("Couldn't find PID of current JVM process.", t);
        }
    }

    private Result callAgent(Task processor) {
        return callAgent(processor, sudoRequired, agentStyle);
    }

    private Result callAgent(Task processor, boolean sudoRequired, AgentStyle style) {
        List<String> args = getArguments(sudoRequired, style);
        args.add(AttachMain.class.getName());

        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        BufferedReader err = null;
        Process agentProcess = null;
        try {
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
            agentProcess = null;

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
        } catch (Throwable t) {
            throw new SASupportException(t.getMessage(), t);
        } finally {
            IOUtils.safelyClose(out);
            IOUtils.safelyClose(in);
            IOUtils.safelyClose(err);

            if (agentProcess != null) {
                agentProcess.destroy();
            }
        }
    }

    private void senseAccess(AgentStyle style) {
        List<String> args = getArguments(false, style);
        args.add(SenseAccessMain.class.getName());

        Process agentProcess = null;
        try {
            agentProcess = new ProcessBuilder(args).start();

            int exitCode = agentProcess.waitFor();
            agentProcess = null;

            if (exitCode != 0) {
                throw new SASupportException("Sense failed.");
            }
        } catch (Throwable t) {
            throw new SASupportException(t.getMessage(), t);
        } finally {
            if (agentProcess != null) {
                agentProcess.destroy();
            }
        }
    }

    private List<String> getArguments(boolean sudoRequired, AgentStyle style) {
        List<String> args = new ArrayList<>();
        if (sudoRequired) {
            args.add("sudo");
        }

        args.add(normalizePath(System.getProperty("java.home")) + "/" + "bin" + "/" + "java");

        switch (style) {
            case NONE:
            case JDK_8:
                break;
            case JDK_9:
                args.add("--add-modules"); args.add("jdk.hotspot.agent");
                args.add("--add-exports"); args.add("jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED");
                args.add("--add-exports"); args.add("jdk.hotspot.agent/sun.jvm.hotspot.runtime=ALL-UNNAMED");
                args.add("--add-exports"); args.add("jdk.hotspot.agent/sun.jvm.hotspot.memory=ALL-UNNAMED");
                break;
            default:
                throw new IllegalStateException("Unhandled style: " + style);
        }

        String classPath = normalizePath(ManagementFactory.getRuntimeMXBean().getClassPath());
        switch (style) {
            case NONE:
                break;
            case JDK_8:
                File hotspotAgentLib = new File(normalizePath(System.getProperty("java.home")) + "/../lib/sa-jdi.jar");
                classPath = classPath + File.pathSeparator + normalizePath(hotspotAgentLib.getAbsolutePath());
                break;
            case JDK_9:
                break;
            default:
                throw new IllegalStateException("Unhandled style: " + style);
        }

        if (!classPath.isEmpty()) {
            args.add("-cp");
            args.add(classPath);
        }
        return args;
    }

    public UniverseData getUniverseData() {
        return (UniverseData) callAgent(new UniverseTask());
    }

    enum AgentStyle {
        NONE,
        JDK_8,
        JDK_9,
    }

}
