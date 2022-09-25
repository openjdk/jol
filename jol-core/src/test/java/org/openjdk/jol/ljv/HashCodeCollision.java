/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jol.ljv;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;


/**
 * @author Ivan Ponomarev
 */
public class HashCodeCollision {

    public List<String> genCollisionString(Integer amount) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            StringBuilder val = new StringBuilder();
            int mask = 1;
            do {
                val.append((i & mask) > 0 ? "BB" : "Aa");
                mask *= 2;
            } while (mask < amount);
            result.add(val.toString());
        }
        return result;
    }

    @Test
    public void testCollisions() {
        for (int number = 1; number < 101; number++) {
            List<String> strings = genCollisionString(number);
            Set<String> deduplicated = new HashSet<>(strings);
            //All strings are unique
            assertEquals(number, deduplicated.size());
            int hashCode = strings.get(0).hashCode();
            for (int i = 1; i < strings.size(); i++) {
                //All strings have the same hash code
                assertEquals(hashCode, strings.get(i).hashCode());
            }
        }
    }
}

