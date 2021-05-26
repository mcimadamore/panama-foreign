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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class ResourceList {

    static final VarHandle HEAD;

    static {
        try {
            HEAD = MethodHandles.lookup().findVarHandle(ResourceList.class, "head", Node.class);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError();
        }
    }

    Node head;

    public static abstract class Node {
        Node rest;

        public abstract void cleanup();

        static Node ofRunnable(Runnable r) {
            return new Node() {
                @Override
                public void cleanup() {
                    r.run();
                }
            };
        }
    }

    void addConfined(Node newNode) {
        newNode.rest = head;
        head = newNode;
    }

    void addAtomic(Node newNode) {
        while (true) {
            Node prev = (Node)HEAD.getAcquire(this);
            newNode.rest = prev;
            if ((Node)HEAD.compareAndExchangeRelease(this, prev, newNode) == prev) {
                return; //victory
            }
            // keep trying
        }
    }

    void cleanup() {
        var current = head;
        while (current != null) {
            current.cleanup();
            current = current.rest;
        }
        head = null;
    }
}
