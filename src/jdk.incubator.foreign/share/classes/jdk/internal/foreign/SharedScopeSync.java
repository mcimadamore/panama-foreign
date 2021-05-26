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

import jdk.incubator.foreign.ResourceScope;
import jdk.internal.misc.ScopedMemoryAccess;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;

public class SharedScopeSync extends ResourceScopeImpl implements Runnable {

    private static final ScopedMemoryAccess SCOPED_MEMORY_ACCESS = ScopedMemoryAccess.getScopedMemoryAccess();

    final ResourceList resourceList = new ResourceList();
    volatile ResourceList resourceListToAdd = resourceList;
    volatile int lockCount;
    volatile boolean closed;

    //private static final VarHandle STATE;

    static {
        try {
            //        STATE = MethodHandles.lookup().findVarHandle(SharedScope.class, "state", int.class);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public SharedScopeSync(Cleaner cleaner) {
        if (cleaner != null) {
            var localList = resourceList;
            cleaner.register(this, () -> localList.cleanup());
        }
    }

    @Override
    public boolean isAlive() {
        return !closed;
    }

    @Override
    public Thread ownerThread() {
        return null;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }

    @Override
    synchronized public void close() {
        if (closed) {
            throw new IllegalStateException("Already closed!");
        }
        if (lockCount > 0) {
            throw new IllegalStateException("Scope is acquired by " + lockCount + " locks");
        }
        closed = true;
        resourceList.cleanup();
    }

    @Override
    synchronized public void addCloseAction(Runnable runnable) {
        checkValidStateSlow();
        resourceListToAdd = resourceListToAdd.add(runnable);
    }

    @Override
    public void bindTo(ResourceScope scope) {
        acquire();
        scope.addCloseAction(this);
    }

    @Override
    public void checkValidState() {
        if (closed) {
            throw ScopedMemoryAccess.Scope.ScopedAccessError.INSTANCE;
        }
    }

    synchronized private void acquire() {
        checkValidStateSlow();
        lockCount++;
    }

    synchronized private void release() {
        lockCount--;
    }

//    private void acquire() {
//        int value;
//        do {
//            value = (int) STATE.getVolatile(this);
//            if (value < ALIVE) {
//                //segment is not alive!
//                throw new IllegalStateException("Already closed");
//            } else if (value == MAX_FORKS) {
//                //overflow
//                throw new IllegalStateException("Segment acquire limit exceeded");
//            }
//        } while (!STATE.compareAndSet(this, value, value + 1));
//    }
//
//    private void release() {
//        int value;
//        do {
//            value = (int) STATE.getVolatile(this);
//            if (value <= ALIVE) {
//                //cannot get here - we can't close segment twice
//                throw new IllegalStateException("Already closed");
//            }
//        } while (!STATE.compareAndSet(this, value, value - 1));
//    }

    @Override
    public void run() {
        release();
    }
}
