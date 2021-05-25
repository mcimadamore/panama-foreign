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
import jdk.internal.ref.CleanerFactory;

import java.lang.ref.Reference;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ImplicitScope extends ResourceScopeImpl implements Runnable {

    private final ConcurrentLinkedQueue<Runnable> resourceList = new ConcurrentLinkedQueue<>();

    public ImplicitScope() {
        Queue<Runnable> localList = resourceList;
        CleanerFactory.cleaner().register(this, () -> cleanup(localList));
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public Thread ownerThread() {
        return null;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Scope cannot be closed");
    }

    @Override
    public void addCloseAction(Runnable runnable) {
        resourceList.add(runnable);
    }

    @Override
    public void bindTo(ResourceScope scope) {
        scope.addCloseAction(this);
    }

    @Override
    public void checkValidState() {
        // do nothing
    }

    @Override
    public void run() {
        Reference.reachabilityFence(this);
    }

    static void cleanup(Queue<Runnable> resourceList) {
        Iterator<Runnable> it = resourceList.iterator();
        while (it.hasNext()) {
            it.next().run();
            it.remove();
        }
    }
}
