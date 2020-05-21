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

package jdk.internal.foreign;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.StampedLock;

/**
 * This class manages the temporal bounds associated with a memory segment as well
 * as thread confinement.
 */
abstract class MemoryScope {

    private static final Unsafe U = Unsafe.getUnsafe();

    final Object ref;
    final Runnable cleanupAction;
    boolean isAlive = true;

    static final long IS_ALIVE_OFFSET = U.objectFieldOffset(MemoryScope.class, "isAlive");

    private static final VarHandle IS_ALIVE;

    static {
        try {
            IS_ALIVE = MethodHandles.lookup().findVarHandle(MemoryScope.class, "isAlive", boolean.class);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    MemoryScope(Runnable cleanupAction, Object ref) {
        this.cleanupAction = cleanupAction;
        this.ref = ref;
    }

    abstract void checkValidState();

    abstract void close();

    abstract MemoryScope dup(Thread owner);

    abstract Thread ownerThread();

    public boolean isAlive() {
        return (boolean)IS_ALIVE.getVolatile(this);
    }

    static class ConfinedMemoryScope extends MemoryScope {

        private final Thread owner;

        ConfinedMemoryScope(Thread owner, Runnable cleanupAction, Object ref) {
            super(cleanupAction, ref);
            this.owner = owner;
        }

        @Override
        void checkValidState() {
            if (owner != null && owner != Thread.currentThread()) {
                throw new IllegalStateException("Attempted access outside owning thread");
            } else if (!isAlive) {
                throw new IllegalStateException("This scope is already closed");
            }
        }

        @Override
        void close() {
            isAlive = false;
            if (cleanupAction != null) {
                cleanupAction.run();
            }
        }

        @Override
        MemoryScope dup(Thread owner) {
            if (owner == this.owner) {
                throw new IllegalArgumentException("Segment already owned by thread: " + owner);
            }
            isAlive = false;
            return owner != null ?
                    new ConfinedMemoryScope(owner, cleanupAction, ref) :
                    new SharedMemoryScope(cleanupAction, ref);
        }

        @Override
        Thread ownerThread() {
            return owner;
        }
    }

    static class SharedMemoryScope extends MemoryScope {

        SharedMemoryScope(Runnable cleanupAction, Object ref) {
            super(cleanupAction, ref);
        }

        @Override
        void checkValidState() {
            if (!isAlive) {
                throw new IllegalStateException("This scope is already closed");
            }
        }

        @Override
        void close() {
            if (!IS_ALIVE.compareAndSet(this, true, false)) {
                throw new IllegalStateException("Segment is not alive");
            }
            U.synchronizeThreads();
            if (cleanupAction != null) {
                cleanupAction.run();
            }
        }

        @Override
        MemoryScope dup(Thread owner) {
            if (owner == null) {
                throw new IllegalStateException("Segment is already shared");
            }
            if (!IS_ALIVE.compareAndSet(this, true, false)) {
                throw new IllegalStateException("Segment is not alive");
            }
            U.synchronizeThreads();
            return owner != null ?
                    new ConfinedMemoryScope(owner, cleanupAction, ref) :
                    new SharedMemoryScope(cleanupAction, ref);
        }

        @Override
        Thread ownerThread() {
            return null;
        }
    }

    static MemoryScope ofConfined(Runnable cleanupAction, Object ref) {
        return ofConfined(Thread.currentThread(), cleanupAction, ref);
    }

    static MemoryScope ofConfined(Thread owner, Runnable cleanupAction, Object ref) {
        return new ConfinedMemoryScope(owner, cleanupAction, ref);
    }

    static MemoryScope ofShared(Runnable cleanupAction, Object ref) {
        return new SharedMemoryScope(cleanupAction, ref);
    }
}