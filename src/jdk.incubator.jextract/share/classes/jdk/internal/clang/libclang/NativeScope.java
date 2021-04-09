/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.clang.libclang;
// Generated by jextract

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

public class NativeScope implements SegmentAllocator, AutoCloseable {
    final ResourceScope resourceScope;
    final ResourceScope.Handle scopeHandle;
    final SegmentAllocator allocator;

    long allocatedBytes = 0;

    private NativeScope() {
        this.resourceScope = ResourceScope.newConfinedScope();
        this.scopeHandle = resourceScope.acquire();
        this.allocator = SegmentAllocator.arenaAllocator(resourceScope);
    }

    private NativeScope(long size) {
        this.resourceScope = ResourceScope.newConfinedScope();
        this.scopeHandle = resourceScope.acquire();
        this.allocator = SegmentAllocator.arenaAllocator(size, resourceScope);
    }

    @Override
    public MemorySegment allocate(long bytesSize, long bytesAlignment) {
        allocatedBytes += bytesSize;
        return allocator.allocate(bytesSize, bytesAlignment);
    }

    public ResourceScope scope() {
        return resourceScope;
    }

    public long allocatedBytes() {
        return allocatedBytes;
    }

    @Override
    public void close() {
        scopeHandle.close();
        resourceScope.close();
    }

    public static NativeScope unboundedScope() {
        return new NativeScope();
    }

    public static NativeScope boundedScope(long size) {
        return new NativeScope(size);
    }
}
