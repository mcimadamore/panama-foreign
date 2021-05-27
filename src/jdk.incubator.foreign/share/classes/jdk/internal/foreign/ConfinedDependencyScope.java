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

public class ConfinedDependencyScope extends ResourceScopeImpl {

    public ConfinedDependencyScope() {
        super(null);
    }

    ResourceScopeImpl scope1, scope2, scope3, scope4, scope5;
    int size;

    @Override
    public boolean isAlive() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Thread ownerThread() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isImplicit() {
        return false;
    }

    @Override
    @SuppressWarnings("fallthrough")
    public void close() {
        switch (size) {
            case 5: scope5.release();
            case 4: scope4.release();
            case 3: scope3.release();
            case 2: scope2.release();
            case 1: scope1.release();
        }
    }

    @Override
    public void checkValidState() {
        throw new AssertionError(); // we should not get here
    }

    @Override
    void addInternal(ResourceList.Node node, boolean isCloseDependency) {
        throw new UnsupportedOperationException();
    }

    public void registerScopeIfNeeded(ResourceScopeImpl scope) {
        if (!scope.isImplicit() && !hasAcquired(scope)) {
            switch (size) {
                case 0 -> scope1 = scope;
                case 1 -> scope2 = scope;
                case 2 -> scope3 = scope;
                case 3 -> scope4 = scope;
                case 4 -> scope5 = scope;
                default -> throw new UnsupportedOperationException();
            }
            scope.acquire();
            size++;
        }
    }

    @SuppressWarnings("fallthrough")
    private boolean hasAcquired(ResourceScopeImpl scope) {
        switch (size) {
            case 5: if (scope5 == scope) return true;
            case 4: if (scope4 == scope) return true;
            case 3: if (scope3 == scope) return true;
            case 2: if (scope2 == scope) return true;
            case 1: if (scope1 == scope) return true;
            default: return false;
        }
    }

    @Override
    void acquire() {
        throw new UnsupportedOperationException();
    }

    @Override
    void release() {
        throw new UnsupportedOperationException();
    }
}
