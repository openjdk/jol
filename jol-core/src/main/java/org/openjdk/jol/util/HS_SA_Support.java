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
package org.openjdk.jol.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.openjdk.jol.util.sa.HS_SA_Processor;
import org.openjdk.jol.util.sa.HS_SA_Result;
import org.openjdk.jol.util.sa.impl.HS_SA_Util;
import org.openjdk.jol.util.sa.impl.compressedrefs.HS_SA_CompressedReferencesProcessor;
import org.openjdk.jol.util.sa.impl.compressedrefs.HS_SA_CompressedReferencesResult;

import sun.management.VMManagement;

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
 * @see HS_SA_Processor
 * @see HS_SA_Result
 *
 * @see HS_SA_CompressedReferencesProcessor
 * @see HS_SA_CompressedReferencesResult
 *
 * @author Serkan Ozal
 */
public class HS_SA_Support {

    private static final String SKIP_HOTSPOT_SA_INIT_FLAG = "jol.skipHotspotSAInit";
    private static final String SKIP_HOTSPOT_SA_ATTACH_FLAG = "jol.skipHotspotSAAttach";
    private static final String TRY_WITH_SUDO_FLAG = "jol.tryWithSudo";

    private static final int DEFAULT_TIMEOUT_IN_MSECS = 5000; // 5 seconds
    private static final int VM_CHECK_PERIOD_SENSITIVITY_IN_MSECS = 1000; // 1 seconds
    private static final int PROCESS_ATTACH_FAILED_EXIT_CODE = 128;

    private static final boolean enable;
    private static final int processId;
    private static final String classpathForAgent;
    private static final boolean sudoRequired;
    private static final String errorMessage;

    static {
        boolean active = true;
        int currentProcId = -1;
        String classpathForAgentProc = null;
        String errorMsg = null;
        boolean sudoNeeded = false;

        if (!Boolean.getBoolean(SKIP_HOTSPOT_SA_INIT_FLAG)) {
            if (Boolean.getBoolean(SKIP_HOTSPOT_SA_ATTACH_FLAG)) {
                active = false;
                errorMsg = "HotSpot Serviceability Agent attach skipped due to " + SKIP_HOTSPOT_SA_ATTACH_FLAG + " flag.";
            } else {
                final String jvmName = System.getProperty("java.vm.name").toLowerCase();
                // Hotspot Serviceability Agent is only supported on Hotspot JVM
                if (!jvmName.contains("hotspot") && !jvmName.contains("openjdk")) {
                    active = false;
                    errorMsg = "HotSpot Serviceability Agent is only supported on HotSpot JVM.";
                } else {
                    try {
                        // Find current process id to connect via HotSpot agent
                        RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();

                        Field jvmField = mxbean.getClass().getDeclaredField("jvm");
                        jvmField.setAccessible(true);

                        VMManagement management = (VMManagement) jvmField.get(mxbean);
                        Method method = management.getClass().getDeclaredMethod("getProcessId");
                        method.setAccessible(true);

                        currentProcId = (Integer) method.invoke(management);
                    } catch (Throwable t) {
                        active = false;
                        errorMsg = "Couldn't find PID of current JVM process.";
                    }
                }

                final String currentClasspath = normalizePath(ManagementFactory.getRuntimeMXBean().getClassPath());
                try {
                    // Search it at classpath
                    Class.forName(HS_SA_Util.HOTSPOT_AGENT_CLASSNAME);

                    // Use current classpath for agent process
                    classpathForAgentProc = currentClasspath;
                } catch (ClassNotFoundException e1) {
                    try {
                        // If it couldn't be found at classpath, try to find it at
                        File hotspotAgentLib = new File(normalizePath(System.getProperty("java.home")) + "/../lib/sa-jdi.jar");
                        if (hotspotAgentLib.exists()) {
                            classpathForAgentProc = currentClasspath + File.pathSeparator +
                                                    normalizePath(hotspotAgentLib.getAbsolutePath());
                        } else {
                            active = false;
                            errorMsg = "Couldn't find HotSpot Serviceability Agent library (sa-jdi.jar).";
                        }
                    } catch (Throwable t2) {
                        active = false;
                        errorMsg = "Couldn't find HotSpot Serviceability Agent library (sa-jdi.jar).";
                    }
                }
            }

            if (active) {
                try {
                    // First check attempt for HotSpot agent connection without "sudo" command
                    executeOnHotspotSAInternal(currentProcId, classpathForAgentProc, false, null, DEFAULT_TIMEOUT_IN_MSECS);
                } catch (ProcessAttachFailedException e1) {
                    // Possibly because of insufficient privilege. So "sudo" is required.
                    // So if "sudo" command is valid on OS and user allows "sudo" usage
                    if (isSudoValidOS() && Boolean.getBoolean(TRY_WITH_SUDO_FLAG)) {
                        try {
                            // Second check attempt for HotSpot agent connection but this time with "sudo" command
                            executeOnHotspotSAInternal(currentProcId, classpathForAgentProc, true, null, DEFAULT_TIMEOUT_IN_MSECS);

                            sudoNeeded = true;
                        } catch (Throwable t2) {
                            active = false;
                            errorMsg = "Unable to attach Serviceability Agent even with super-user privileges: " + t2.getMessage();
                        }
                    } else {
                        active = false;
                        errorMsg = "Unable to attach Serviceability Agent. You can try again with super-user privileges. Use -D" + TRY_WITH_SUDO_FLAG + "=true to try with sudo.";
                    }
                } catch (Throwable t1) {
                    active = false;
                    errorMsg = "Unable to attach Serviceability Agent: " + t1.getMessage();
                }
            }
        }

        enable = active;
        processId = currentProcId;
        classpathForAgent = classpathForAgentProc;
        errorMessage = "WARNING: " + errorMsg;
        sudoRequired = sudoNeeded;
    }

    private HS_SA_Support() {

    }

    private static String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    private static void checkEnable() {
        if (!enable) {
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * <p>
     * Checks and gets the condition about if "sudo" command is required for creating external
     * Java process to connect current process as HotSpot agent.
     * </p>
     *
     * <p>
     * On some UNIX based and MacOSX based operations systems, HotSpot Serviceability Agent (SA) process
     * attach fails due to insufficient privilege. So these processes must be execute as super user.
     * </p>
     *
     * <pre>
     * See also JVM Bug reports:
     *
     *      <a href="http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7129704">http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7129704</a>
     *      <a href="http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7050524">http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7050524</a>
     *      <a href="http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7112802">http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7112802</a>
     *      <a href="http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7160774">http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7160774</a>
     *
     *      <a href="https://bugs.openjdk.java.net/browse/JDK-7129704">https://bugs.openjdk.java.net/browse/JDK-7129704</a>
     *      <a href="https://bugs.openjdk.java.net/browse/JDK-7050524">https://bugs.openjdk.java.net/browse/JDK-7050524</a>
     *      <a href="https://bugs.openjdk.java.net/browse/JDK-7112802">https://bugs.openjdk.java.net/browse/JDK-7112802</a>
     *      <a href="https://bugs.openjdk.java.net/browse/JDK-7160774">https://bugs.openjdk.java.net/browse/JDK-7160774</a>
     * </pre>
     *
     * @return <code>true</code> if "sudo" command is required, otherwise <code>false</code>
     */
    private static boolean isSudoValidOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) { // UNIX based operation system
            return true;
        } else if (osName.contains("mac")) { // MacOSX based operation system
            return true;
        } else {
            return false;
        }
    }

    private static void safelyClose(OutputStream out) {
        if (out != null) {
            try {
                out.flush();
            } catch (IOException e) {
                // ignore
            }
            try {
                out.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static void safelyClose(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static void safelyClose(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static HS_SA_Result executeOnHotspotSAInternal(HS_SA_Processor processor, int timeoutInMsecs) {
        checkEnable();

        return executeOnHotspotSAInternal(processId, classpathForAgent, sudoRequired, processor, timeoutInMsecs);
    }

    private static HS_SA_Result executeOnHotspotSAInternal(int procId, String classpath, boolean sudoRequired,
            HS_SA_Processor processor, int timeoutInMsecs) {
        // Generate required arguments to create an external Java process
        List<String> args = new ArrayList<String>();
        if (sudoRequired) {
            args.add("sudo");
        }
        args.add(normalizePath(System.getProperty("java.home")) + "/" + "bin" + "/" + "java");
        args.add("-D" + SKIP_HOTSPOT_SA_INIT_FLAG + "=true"); // For preventing infinite loop if attaching process touches this class
        args.add("-cp");
        args.add(classpath);
        args.add(HS_SA_Support.class.getName());

        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        BufferedReader err = null;
        Process agentProcess = null;
        try {
            // Create an external Java process to connect this process as HotSpot agent
            agentProcess = new ProcessBuilder(args).start();

            HS_SA_Request request = new HS_SA_Request(procId, processor, timeoutInMsecs);

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
                                                       "(id=" + procId + ") from external process failed");
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
            HS_SA_Response response = (HS_SA_Response) in.readObject();

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
            throw new RuntimeException(t.getMessage(), t);
        } finally {
            safelyClose(out);
            safelyClose(in);
            safelyClose(err);

            if (agentProcess != null) {
                // If process is still in use, destroy it.
                // When it has terminated, it is set to "null" after "waitFor".
                agentProcess.destroy();
            }
        }
    }

    private static HS_SA_Processor createInstance(Class<? extends HS_SA_Processor> processorClass) {
        try {
            return processorClass.newInstance();
        } catch (Throwable t) {
            throw new IllegalArgumentException("Could not create instance of " + processorClass.getName(), t);
        }
    }

    /**
     * Specific exception type to represent process attach fail cases.
     */
    @SuppressWarnings("serial")
    private static class ProcessAttachFailedException extends RuntimeException {

        private ProcessAttachFailedException(String message) {
            super(message);
        }

    }

    /**
     * Represents request to HotSpot agent process by holding process id, timeout and {@link HS_SA_Processor} to execute.
     */
    @SuppressWarnings("serial")
    private static class HS_SA_Request implements Serializable {

        private final int processId;
        private final HS_SA_Processor processor;
        private final int timeout;

        private HS_SA_Request(int processId, HS_SA_Processor processor) {
            this.processId = processId;
            this.processor = processor;
            this.timeout = DEFAULT_TIMEOUT_IN_MSECS;
        }

        private HS_SA_Request(int processId, HS_SA_Processor processor, int timeout) {
            this.processId = processId;
            this.processor = processor;
            this.timeout = timeout;
        }

        public int getProcessId() {
            return processId;
        }

        public HS_SA_Processor getProcessor() {
            return processor;
        }

        public int getTimeout() {
            return timeout;
        }

    }

    /**
     * Represents response from HotSpot agent process by holding result and error if occurred.
     */
    @SuppressWarnings("serial")
    private static class HS_SA_Response implements Serializable {

        private final HS_SA_Result result;
        private final Throwable error;

        private HS_SA_Response(HS_SA_Result result) {
            this.result = result;
            this.error = null;
        }

        private HS_SA_Response(Throwable error) {
            this.result = null;
            this.error = error;
        }

        public HS_SA_Result getResult() {
            return result;
        }

        public Throwable getError() {
            return error;
        }

    }

    public static void main(final String[] args) {
        HS_SA_Response response = null;
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

            final HS_SA_Request request = (HS_SA_Request) in.readObject();

            final Class<?> hotspotAgentClass = HS_SA_Util.getHotspotAgentClass();
            hotspotAgent = HS_SA_Util.createHotspotAgentInstance();
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
                Thread.sleep(VM_CHECK_PERIOD_SENSITIVITY_IN_MSECS); // Wait a little before an attempt
                try {
                    if ((vm = HS_SA_Util.getVMInstance()) != null) {
                        break;
                    }
                } catch (Throwable err) {
                    // There is nothing to do, try another
                }
            }

            // Check about if VM is initialized and ready to use
            if (vm != null) {
                final HS_SA_Processor processor = request.getProcessor();
                if (processor != null) {
                    // Execute processor and gets its result
                    final HS_SA_Result result = processor.process();
                    response = new HS_SA_Response(result);
                }
            } else {
                throw new IllegalStateException("VM couldn't be initialized !");
            }
        } catch (Throwable t) {
            // If there is an error, attach it to response
            response = new HS_SA_Response(t);
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

    /**
     * Returns <code>true</code> if HotSpot Serviceability Agent support is enable, otherwise <code>false</code>.
     *
     * @return the enable state of HotSpot Serviceability Agent support
     */
    public static boolean isEnable() {
        return enable;
    }

    /**
     * Executes given typed {@link HS_SA_Processor} on HotSpot agent process and
     * returns a {@link HS_SA_Result} instance as result.
     *
     * @param processorClass the type of {@link HS_SA_Processor} instance to execute
     * @return the {@link HS_SA_Result} instance as result of processor execution
     */
    public static HS_SA_Result executeOnHotspotSA(Class<? extends HS_SA_Processor> processorClass) {
        return executeOnHotspotSA(createInstance(processorClass), DEFAULT_TIMEOUT_IN_MSECS);
    }

    /**
     * Executes given {@link HS_SA_Processor} on HotSpot agent process and
     * returns a {@link HS_SA_Result} instance as result.
     *
     * @param processor the {@link HS_SA_Processor} instance to execute
     * @return the {@link HS_SA_Result} instance as result of processor execution
     */
    public static HS_SA_Result executeOnHotspotSA(HS_SA_Processor processor) {
        return executeOnHotspotSAInternal(processor, DEFAULT_TIMEOUT_IN_MSECS);
    }

    /**
     * Executes given typed {@link HS_SA_Processor} on Hotspot agent process and
     * returns a {@link HS_SA_Result} instance as result.
     *
     * @param processorClass    the type of {@link HS_SA_Processor} instance to execute
     * @param timeoutInMsecs    the timeout in milliseconds to wait at most for terminating connection between
     *                          current process and Hotspot agent process.
     * @return the {@link HS_SA_Result} instance as result of processor execution
     */
    public static HS_SA_Result executeOnHotspotSA(Class<? extends HS_SA_Processor> processorClass, int timeoutInMsecs) {
        return executeOnHotspotSA(createInstance(processorClass), timeoutInMsecs);
    }

    /**
     * Executes given {@link HS_SA_Processor} on Hotspot agent process and
     * returns a {@link HS_SA_Result} instance as result.
     *
     * @param processor         the {@link HS_SA_Processor} instance to execute
     * @param timeoutInMsecs    the timeout in milliseconds to wait at most for terminating connection between
     *                          current process and Hotspot agent process.
     * @return the {@link HS_SA_Result} instance as result of processor execution
     */
    public static HS_SA_Result executeOnHotspotSA(HS_SA_Processor processor, int timeoutInMsecs) {
        return executeOnHotspotSAInternal(processor, timeoutInMsecs);
    }

    /**
     * Gets the compressed references information as {@link HS_SA_CompressedReferencesResult} instance.
     *
     * @return the compressed references information as {@link HS_SA_CompressedReferencesResult} instance
     */
    public static HS_SA_CompressedReferencesResult getCompressedReferences() {
        return (HS_SA_CompressedReferencesResult) executeOnHotspotSA(HS_SA_CompressedReferencesProcessor.class);
    }

    /**
     * Gives details about Hotspot Serviceability Agent support.
     *
     * @return the string representation of Hotspot Serviceability Agent support
     */
    public static String details() {
        checkEnable();

        return "HotspotServiceabilityAgentSupport [" +
                    "enable=" + enable + ", " +
                    "processId=" + processId + ", " +
                    "classpathForAgent=" + classpathForAgent + ", " +
                    "errorMessage=" + errorMessage + "]";
    }

}
