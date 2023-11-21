/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

import java.util.*;

/**
 * Naive HashMultimap.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class Multimap<K, V> {

    private final Map<K, List<V>> map = new HashMap<>();

    public void put(K k, V v) {
        List<V> vs = map.get(k);
        if (vs == null) {
            vs = new ArrayList<>();
            map.put(k, vs);
        }
        vs.add(v);
    }

    public void putEmpty(K k) {
        List<V> vs = map.get(k);
        if (vs == null) {
            vs = new ArrayList<>();
            map.put(k, vs);
        }
    }

    public List<V> get(K k) {
        if (map.containsKey(k)) {
            return Collections.unmodifiableList(map.get(k));
        } else {
            return Collections.emptyList();
        }
    }

    public Collection<K> keys() {
        return map.keySet();
    }

    public List<V> remove(K k) {
        return map.remove(k);
    }

    public boolean contains(K k) {
        return map.containsKey(k);
    }
}
