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

import java.io.Serializable;

/**
 * <p>
 * Interface for processors which do some stuff via Hotspot Serviceability Agent API on Hotspot internals.
 * </p>
 *
 * <p>
 * {@link Task} implementations must be fully (including its fields) serializable.
 * So if there is any field will not be serialized, it must be ignored or serialization logic must be customized.
 * Please see <a href="http://www.oracle.com/technetwork/articles/java/javaserial-1536170.html">http://www.oracle.com/technetwork/articles/java/javaserial-1536170.html</a> for more details.
 * </p>
 *
 * @see Result
 *
 * @author Serkan Ozal
 */
interface Task extends Serializable {

    /**
     * Processes {@link Task}'s own logic over Hotspot Serviceability Agent.
     *
     * @return the {@link Result} instance as result
     */
    Result process();

}
