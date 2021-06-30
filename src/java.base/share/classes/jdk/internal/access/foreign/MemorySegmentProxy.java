/*
 *  Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package jdk.internal.access.foreign;

import jdk.internal.misc.ScopedMemoryAccess;

/**
 * This abstract class is required to allow implementations of the {@code MemorySegment} interface (which is defined inside
 * an incubating module) to be accessed from the memory access var handles.
 */
public abstract class MemorySegmentProxy {
    /**
     * Check that memory access is within spatial bounds and that access is compatible with segment access modes.
     * @throws UnsupportedOperationException if underlying segment has incompatible access modes (e.g. attempting to write
     * a read-only segment).
     * @throws IndexOutOfBoundsException if access is out-of-bounds.
     */
    public abstract void checkAccess(long offset, long length, boolean readOnly);
    public abstract long unsafeGetOffset();
    public abstract Object unsafeGetBase();
    public abstract ScopedMemoryAccess.Scope scope();
}
