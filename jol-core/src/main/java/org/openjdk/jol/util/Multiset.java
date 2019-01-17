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
package org.openjdk.jol.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Naive HashMultiset.
 *
 * @param <T> element type
 * @author Aleksey Shipilev
 */
public class Multiset<T> {

    private final Map<T, Long> map = new HashMap<>();

    public void add(T t) {
        add(t, 1);
    }

    public void add(T key, long count) {
        Long v = map.get(key);
        if (v == null) {
            v = 0L;
        }
        v += count;
        map.put(key, v);
    }

    public long count(T key) {
        Long v = map.get(key);
        return (v == null) ? 0 : v;
    }

    public Collection<T> keys() {
        return map.keySet();
    }

    public long size() {
        long size = 0;
        for (T k : keys()) {
            size += count(k);
        }
        return size;
    }

    public void merge(Multiset<T> other) {
        for (T key : other.keys()) {
            add(key, other.count(key));
        }
    }
}
