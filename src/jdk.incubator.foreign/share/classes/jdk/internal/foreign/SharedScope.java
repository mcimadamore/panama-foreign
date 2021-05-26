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
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SharedScope extends ResourceScopeImpl implements Runnable {

    private static final ScopedMemoryAccess SCOPED_MEMORY_ACCESS = ScopedMemoryAccess.getScopedMemoryAccess();

    private static final int ALIVE = 0;
    private static final int CLOSING = -1;
    private static final int CLOSED = -2;
    private static final int MAX_FORKS = Integer.MAX_VALUE;

    private int state = ALIVE;
    private final ConcurrentLinkedQueue<Runnable> resourceList = new ConcurrentLinkedQueue<>();

    private static final VarHandle STATE;

    static {
        try {
            STATE = MethodHandles.lookup().findVarHandle(SharedScope.class, "state", int.class);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public SharedScope(Cleaner cleaner) {
        if (cleaner != null) {
            Queue<Runnable> localList = resourceList;
            cleaner.register(this, () -> cleanup(localList));
        }
    }

    @Override
    public boolean isAlive() {
        return (int) STATE.getVolatile(this) != CLOSED;
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
    public void close() {
        int prevState = (int) STATE.compareAndExchange(this, ALIVE, CLOSING);
        if (prevState < 0) {
            throw new IllegalStateException("Already closed");
        } else if (prevState != ALIVE) {
            throw new IllegalStateException("Scope is acquired by " + prevState + " locks");
        }
        // avoid synchronization costs if there's no memory segment attached to this scope
        boolean success = true;//SCOPED_MEMORY_ACCESS.closeScope(this);
        STATE.setVolatile(this, success ? CLOSED : ALIVE);
        if (!success) {
            throw new IllegalStateException("Cannot close while another thread is accessing the segment");
        }
        cleanup(resourceList);
    }

    @Override
    public void addCloseAction(Runnable runnable) {
        acquire();
        resourceList.add(runnable);
        release();
    }

    @Override
    public void bindTo(ResourceScope scope) {
        acquire();
        scope.addCloseAction(this);
    }

    @Override
    public void checkValidState() {
        if (state < ALIVE) {
            throw ScopedMemoryAccess.Scope.ScopedAccessError.INSTANCE;
        }
    }

    private void acquire() {
        int value;
        do {
            value = (int) STATE.getVolatile(this);
            if (value < ALIVE) {
                //segment is not alive!
                throw new IllegalStateException("Already closed");
            } else if (value == MAX_FORKS) {
                //overflow
                throw new IllegalStateException("Segment acquire limit exceeded");
            }
        } while (!STATE.compareAndSet(this, value, value + 1));
    }

    private void release() {
        int value;
        do {
            value = (int) STATE.getVolatile(this);
            if (value <= ALIVE) {
                //cannot get here - we can't close segment twice
                throw new IllegalStateException("Already closed");
            }
        } while (!STATE.compareAndSet(this, value, value - 1));
    }

    @Override
    public void run() {
        release();
    }

    static void cleanup(Queue<Runnable> resourceList) {
        Iterator<Runnable> it = resourceList.iterator();
        while (it.hasNext()) {
            it.next().run();
            it.remove();
        }
    }
}
