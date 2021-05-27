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

package jdk.internal.foreign;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.internal.misc.ScopedMemoryAccess;

import java.lang.ref.Cleaner;
import java.util.Objects;

public abstract class ResourceScopeImpl implements ResourceScope, ScopedMemoryAccess.Scope, SegmentAllocator {

    final ResourceList resourceList = new ResourceList();

    static {
        ScopedMemoryAccess.implicitScopeSupplier = () -> createImplicitScope();
        ScopedMemoryAccess.sharedScopeSupplier = () -> createShared(null);
        ScopedMemoryAccess.confinedScopeSupplier = () -> createConfined(null);
    }

    public ResourceScopeImpl(Cleaner cleaner) {
        if (cleaner != null) {
            var localList = resourceList;
            cleaner.register(this, localList::cleanup);
        }
    }

    public abstract void checkValidState();

    public final void checkValidStateSlow() {
        if (ownerThread() != null && Thread.currentThread() != ownerThread()) {
            throw new IllegalStateException("Attempted access outside owning thread");
        } else if (!isAlive()) {
            throw new IllegalStateException("Already closed");
        }
    }

    @Override
    public void bindTo(ScopedMemoryAccess.Scope scope) {
        addCloseDependency((ResourceScope)scope);
    }

    public static ResourceScopeImpl createImplicitScope() {
        return new ImplicitScope();
    }

    public static ResourceScopeImpl createConfined(Thread thread, Cleaner cleaner) {
        return new ConfinedScope(thread, cleaner);
    }

    /**
     * Creates a confined memory scope with given attachment and cleanup action. The returned scope
     * is assumed to be confined on the current thread.
     * @return a confined memory scope
     */
    public static ResourceScopeImpl createConfined(Cleaner cleaner) {
        return new ConfinedScope(Thread.currentThread(), cleaner);
    }

    /**
     * Creates a shared memory scope with given attachment and cleanup action.
     * @return a shared memory scope
     */
    public static ResourceScopeImpl createShared(Cleaner cleaner) {
        return new SharedScope(cleaner);
    }

    public final void addOrCleanupIfFail(ResourceList.Node node, boolean isCloseDependency) {
        try {
            addInternal(node, isCloseDependency);
        } catch (IllegalStateException ex) {
            node.cleanup();
            throw ex;
        }
    }

    @Override
    public final void addCloseDependency(ResourceScope scope) {
        checkValidStateSlow();
        acquire();
        ((ResourceScopeImpl)scope).addInternal(new ResourceList.Node() {
            @Override
            public void cleanup() {
                ResourceScopeImpl.this.release();
            }
        }, true);
    }

    public abstract void acquire();

    public abstract void release();

    @Override
    public final void addCloseAction(Runnable runnable) {
        Objects.requireNonNull(runnable);
        addInternal(ResourceList.Node.ofRunnable(runnable), false);
    }

    abstract void addInternal(ResourceList.Node node, boolean isCloseDependency);

    public static ImplicitScope GLOBAL = new ImplicitScope() {
        @Override
        public void addInternal(ResourceList.Node runnable, boolean isCloseDependency) {
            // do nothing
        }
    };

    @Override
    public MemorySegment allocate(long bytesSize, long bytesAlignment) {
        return MemorySegment.allocateNative(bytesSize, bytesAlignment, this);
    }
}
