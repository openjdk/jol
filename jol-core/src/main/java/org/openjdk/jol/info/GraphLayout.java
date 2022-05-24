/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jol.info;

import org.openjdk.jol.util.Multiset;
import org.openjdk.jol.util.ObjectUtils;
import org.openjdk.jol.vm.VM;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.List;

/**
 * Holds the object graph layout info.
 */
public class GraphLayout {

    /**
     * Parse the object graph starting from the given instance.
     *
     * @param roots root instances to start from
     * @return object graph
     */
    public static GraphLayout parseInstance(Object... roots) {
        return new GraphWalker().walk(roots);
    }

    private final List<GraphPathRecord> gprs = new ArrayList<>();
    private final String description;

    private volatile boolean processedHisto;
    private Set<Class<?>> classes;
    private Multiset<Class<?>> classSizes;
    private Multiset<Class<?>> classCounts;

    private volatile boolean processedAddresses;
    private Map<Long, GraphPathRecord> addresses;
    private long minAddress;
    private long maxAddress;
    private int addressTries;
    private boolean addressStable;

    private volatile boolean processedTotals;
    private long totalCount;
    private long totalSize;

    public GraphLayout(Object... roots) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Object root : roots) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(", ");
            }
            sb.append(String.format("%s@%xd", root.getClass().getName(), System.identityHashCode(root)));
        }
        this.description = sb.toString();
    }

    void addRecord(GraphPathRecord gpr) {
        gprs.add(gpr);
    }

    private void ensureProcessedAddresses() {
        if (processedAddresses) {
            return;
        }

        synchronized (this) {
            if (gprs.isEmpty()) {
                minAddress = 0;
                maxAddress = 0;
            }

            addresses = new HashMap<>();

            // First round of address computations to seed the addresses.
            // We would then confirm the addresses are stable on the next step.
            long[] rawAddresses = new long[gprs.size()];
            for (int i = 0; i < gprs.size(); i++) {
                rawAddresses[i] = VM.current().addressOf(gprs.get(i).obj());
            }

            boolean good = false;
            for (addressTries = 0; (addressTries < 10) && !good; addressTries++) {
                addresses.clear();
                minAddress = Long.MAX_VALUE;
                maxAddress = Long.MIN_VALUE;

                good = true;
                for (int i = 0; i < gprs.size(); i++) {
                    GraphPathRecord gpr = gprs.get(i);
                    Object o = gpr.obj();

                    long addr = VM.current().addressOf(o);
                    if (rawAddresses[i] != addr) {
                        // If any object have moved, continue traversing to recompute
                        // others, and then force a retry, hoping for a clean iteration.
                        rawAddresses[i] = addr;
                        good = false;
                    }

                    if (good) {
                        addresses.put(addr, gpr);
                        minAddress = Math.min(minAddress, addr);
                        maxAddress = Math.max(maxAddress, addr);
                    }
                }
            }

            addressStable = good;
            processedAddresses = true;
        }
    }

    /**
     * Subtract another layout data from the current one.
     * This method does not change the current data object, but produces another one.
     *
     * Note that the object identity is derived from object addresses: if some objects
     * move, they will be treated as new/absent. You may want to quiesce the heap
     * (possibly doing several back-to-back GCs) before taking the snapshots.
     *
     * @param another data object
     * @return new data object, that contains the difference.
     */
    public GraphLayout subtract(GraphLayout another) {
        ensureProcessedAddresses();
        another.ensureProcessedAddresses();

        GraphLayout res = new GraphLayout();
        for (Map.Entry<Long, GraphPathRecord> e : addresses.entrySet()) {
            if (!another.addresses.containsKey(e.getKey())) {
                res.addRecord(e.getValue());
            }
        }
        return res;
    }

    /**
     * Add another layout data to the current one.
     * This method does not change the current data object, but produces another one.
     *
     * Note that the object identity is derived from object addresses: if some objects
     * move, they will be treated as new. You may want to quiesce the heap
     * (possibly doing several back-to-back GCs) before taking the snapshots.
     *
     * @param another data object
     * @return new data object, that contains the union.
     */
    public GraphLayout add(GraphLayout another) {
        ensureProcessedAddresses();
        another.ensureProcessedAddresses();

        GraphLayout res = new GraphLayout();
        for (Map.Entry<Long, GraphPathRecord> e : addresses.entrySet()) {
            res.addRecord(e.getValue());
        }
        for (Map.Entry<Long, GraphPathRecord> e : another.addresses.entrySet()) {
            if (!addresses.containsKey(e.getKey())) {
                res.addRecord(e.getValue());
            }
        }
        return res;
    }


    private void ensureProcessedHisto() {
        if (processedHisto) {
            return;
        }

        synchronized (this) {
            classes = new TreeSet<>(new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> o1, Class<?> o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            classSizes = new Multiset<>();
            classCounts = new Multiset<>();

            for (GraphPathRecord gpr : gprs) {
                Class<?> klass = gpr.klass();
                classes.add(klass);
                classCounts.add(klass);
                try {
                    classSizes.add(klass, gpr.size());
                } catch (Exception e) {
                    classSizes.add(klass, 0);
                }
            }

            processedHisto = true;
        }
    }

    /**
     * Answer the class sizes.
     *
     * @return class sizes multiset
     */
    public Multiset<Class<?>> getClassSizes() {
        ensureProcessedHisto();
        return classSizes;
    }

    /**
     * Answer the class counts
     *
     * @return class counts multiset
     */
    public Multiset<Class<?>> getClassCounts() {
        ensureProcessedHisto();
        return classCounts;
    }

    /**
     * Answer the set of observed classes
     *
     * @return observed classes set
     */
    public Set<Class<?>> getClasses() {
        ensureProcessedHisto();
        return classes;
    }

    private void ensureProcessedTotals() {
        if (processedTotals) return;

        synchronized (this) {
            for (GraphPathRecord gpr : gprs) {
                totalSize += gpr.size();
            }
            totalCount = gprs.size();

            processedTotals = true;
        }
    }

    /**
     * Answer the total instance count
     *
     * @return total instance count
     */
    public long totalCount() {
        ensureProcessedTotals();
        return totalCount;
    }

    /**
     * Answer the total instance footprint
     *
     * @return total instance footprint, bytes
     */
    public long totalSize() {
        ensureProcessedTotals();
        return totalSize;
    }

    /**
     * Answer the starting address of observed memory chunk
     *
     * @return starting address
     */
    public long startAddress() {
        ensureProcessedAddresses();
        return minAddress;
    }

    /**
     * Answer the ending address of observed memory chunk
     *
     * @return ending address
     */
    public long endAddress() {
        ensureProcessedAddresses();
        return maxAddress;
    }

    /**
     * Answer the set of addresses for the discovered objects
     *
     * @return sorted set of addresses
     * @see #record(long)
     */
    public SortedSet<Long> addresses() {
        ensureProcessedAddresses();
        return new TreeSet<>(addresses.keySet());
    }

    /**
     * Get the object descriptor for the given address
     *
     * @param address address
     * @return object descriptor
     */
    public GraphPathRecord record(long address) {
        ensureProcessedAddresses();
        return addresses.get(address);
    }

    /**
     * Get the stringly representation of footprint table
     *
     * @return footprint table
     */
    public String toFootprint() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(description + " footprint:");
        pw.printf(" %9s %9s %9s   %s%n", "COUNT", "AVG", "SUM", "DESCRIPTION");
        for (Class<?> key : getClasses()) {
            long count = getClassCounts().count(key);
            long size = getClassSizes().count(key);
            pw.printf(" %9d %9d %9d   %s%n", count, size / count, size, key.getName());
        }
        pw.printf(" %9d %9s %9d   %s%n", totalCount(), "", totalSize(), "(total)");
        pw.println();
        pw.close();
        return sw.toString();
    }

    /**
     * Get the stringly representation of object graph
     *
     * @return linearized text form of object graph
     */
    public String toPrintable() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        long last = 0L;

        int typeLen = "TYPE".length();
        for (long addr : addresses()) {
            GraphPathRecord r = record(addr);
            typeLen = Math.max(typeLen, r.klass().getName().length());
        }

        pw.println(description + " object externals:");
        pw.printf(" %16s %10s %-" + typeLen + "s %-30s %s%n", "ADDRESS", "SIZE", "TYPE", "PATH", "VALUE");
        for (long addr : addresses()) {
            GraphPathRecord record = record(addr);
            long size = record.size();

            if (addr > last && last != 0L) {
                pw.printf(" %16x %10d %-" + typeLen + "s %-30s %s%n", last, addr - last, "(something else)", "(somewhere else)", "(something else)");
            }
            if (addr < last) {
                pw.printf(" %16x %10d %-" + typeLen + "s %-30s %s%n", last, addr - last, "**** OVERLAP ****", "**** OVERLAP ****", "**** OVERLAP ****");
            }

            pw.printf(" %16x %10d %-" + typeLen + "s %-30s %s%n", addr, size, record.klass().getName(), record.path(), ObjectUtils.safeToString(record.obj()));
            last = addr + size;
        }
        pw.println();
        pw.println("Addresses are " + (addressStable ? "stable" : "still unstable") + " after " + addressTries + " tries.");
        pw.println();
        pw.close();
        return sw.toString();
    }

    /**
     * Put the graphical representation of object graph into the file.
     *
     * @param fileName filename
     * @throws IOException when I/O fails
     */
    public void toImage(String fileName) throws IOException {
        if (addresses().isEmpty()) {
            return;
        }

        long start = startAddress();
        long end = endAddress() + record(endAddress()).size();

        if (start == end) {
            // Make sure we render something
            end = start + 1;
        }

        final int WIDTH = 1000;
        final int HEIGHT = 320;
        final int GRAPH_HEIGHT = 100;
        final int SCALE_WIDTH = 30;
        final int EXT_PAD = 50;
        final int PAD = 20;

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = image.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        int minDepth = Integer.MAX_VALUE;
        int maxDepth = Integer.MIN_VALUE;
        for (long addr : addresses()) {
            GraphPathRecord p = record(addr);
            minDepth = Math.min(minDepth, p.depth());
            maxDepth = Math.max(maxDepth, p.depth());
        }

        Multiset<Integer> depths = new Multiset<>();
        for (long addr : addresses()) {
            GraphPathRecord r = record(addr);
            depths.add(r.depth(), r.size());
        }

        int lastX = 0;
        for (long addr : addresses()) {
            long size = record(addr).size();

            int x1 = SCALE_WIDTH + EXT_PAD + (int) ((WIDTH - SCALE_WIDTH - EXT_PAD * 2) * (addr - start) / (end - start));
            int x2 = SCALE_WIDTH + EXT_PAD + (int) ((WIDTH - SCALE_WIDTH - EXT_PAD * 2) * (addr + size - start) / (end - start));
            x1 = Math.max(x1, lastX);
            x2 = Math.max(x2, lastX);

            float relDepth = 1.0f * (record(addr).depth() - minDepth) / (maxDepth - minDepth + 1);
            g.setColor(Color.getHSBColor(relDepth, 1.0f, 0.9f));
            g.fillRect(x1, EXT_PAD, x2 - x1, GRAPH_HEIGHT);
        }

        for (int depth = minDepth; depth <= maxDepth; depth++) {
            float relDepth = 1.0f * (depth - minDepth) / (maxDepth - minDepth + 1);
            g.setColor(Color.getHSBColor(relDepth, 1.0f, 0.9f));
            int y1 = HEIGHT * (depth - minDepth) / (maxDepth - minDepth + 1);
            int y2 = HEIGHT * (depth + 1 - minDepth) / (maxDepth - minDepth + 1);
            g.fillRect(0, y1, SCALE_WIDTH, y2 - y1);
        }

        lastX = SCALE_WIDTH + EXT_PAD;
        for (int depth = minDepth; depth <= maxDepth; depth++) {
            int w = (int) ((WIDTH - SCALE_WIDTH - EXT_PAD * 2) * depths.count(depth) / (end - start));

            float relDepth = 1.0f * (depth - minDepth) / (maxDepth - minDepth + 1);
            g.setColor(Color.getHSBColor(relDepth, 1.0f, 0.9f));
            g.fillRect(lastX, GRAPH_HEIGHT + EXT_PAD + PAD, w, GRAPH_HEIGHT);

            lastX += w;
        }

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.0f));
        g.drawRect(SCALE_WIDTH + EXT_PAD, EXT_PAD, WIDTH - EXT_PAD * 2 - SCALE_WIDTH, GRAPH_HEIGHT);
        g.drawRect(SCALE_WIDTH + EXT_PAD, GRAPH_HEIGHT + EXT_PAD + PAD, WIDTH - EXT_PAD * 2 - SCALE_WIDTH, GRAPH_HEIGHT);

        g.setStroke(new BasicStroke(1.0f));
        g.drawLine(SCALE_WIDTH + EXT_PAD, GRAPH_HEIGHT * 2 + EXT_PAD + PAD + PAD, WIDTH - EXT_PAD, GRAPH_HEIGHT * 2 + EXT_PAD + PAD + PAD);
        g.drawLine(SCALE_WIDTH + EXT_PAD, GRAPH_HEIGHT * 2 + EXT_PAD + PAD + PAD - 5, SCALE_WIDTH + EXT_PAD, GRAPH_HEIGHT * 2 + EXT_PAD + PAD + PAD + 5);
        g.drawLine(WIDTH - EXT_PAD, GRAPH_HEIGHT * 2 + EXT_PAD + PAD + PAD - 5, WIDTH - EXT_PAD, GRAPH_HEIGHT * 2 + EXT_PAD + PAD + PAD + 5);

        Font font = new Font("Serif", Font.PLAIN, 18);
        g.setFont(font);

        String labelDense = (end - start) / 1024 + " Kb";

        g.setBackground(Color.WHITE);
        g.setColor(Color.BLACK);
        g.drawString(labelDense, WIDTH / 2 - 50, 2 * GRAPH_HEIGHT + EXT_PAD + 2 * PAD + 20);

        g.drawString(String.format("%s", description), SCALE_WIDTH + EXT_PAD, 30);

        AffineTransform orig = g.getTransform();
        int x = SCALE_WIDTH + EXT_PAD - 5;
        int y1 = GRAPH_HEIGHT + EXT_PAD;
        g.rotate(-Math.toRadians(90.0), x, y1);
        g.drawString("Actual:", x, y1);
        g.setTransform(orig);

        int y2 = 2 * GRAPH_HEIGHT + EXT_PAD + PAD;
        g.rotate(-Math.toRadians(90.0), x, y2);
        g.drawString("Dense:", x, y2);
        g.setTransform(orig);

        ImageIO.write(image, "png", new File(fileName));
    }

}
