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

package org.openjdk.bench.jdk.incubator.foreign;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class TestSharedScopeContention {

    ResourceScope scope;
    ExecutorService executorService;

    static final int THREADS = 10;
    static final int ITERATIONS_PER_THREAD = 100;

    final Supplier<Void> ADD_TO_SCOPE = () -> {
        for (int i = 0 ; i < ITERATIONS_PER_THREAD ; i++) {
            MemorySegment.allocateNative(10, scope);
        }
        return null;
    };

    final Supplier<Void> ACQUIRE_RELEASE = () -> {
        for (int i = 0 ; i < ITERATIONS_PER_THREAD ; i++) {
            try (ResourceScope op = ResourceScope.newConfinedScope()) {
                scope.addCloseDependency(op);
            }
        }
        return null;
    };

    @Setup(Level.Invocation)
    public void setup() {
        scope = ResourceScope.newSharedScope();
        executorService = Executors.newFixedThreadPool(THREADS);
    }


    @TearDown(Level.Invocation)
    public void tearDown() throws InterruptedException {
        scope.close();
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.DAYS);
    }

    @Benchmark
    public void contention_add_to_scope() throws InterruptedException, ExecutionException {
        CompletableFuture<?>[] results = new CompletableFuture[THREADS * 10];
        for (int i = 0 ; i < results.length ; i++) {
            results[i] = CompletableFuture.supplyAsync(ADD_TO_SCOPE, executorService);
        }
        CompletableFuture.allOf(results).get();
    }

    @Benchmark
    public void contention_acquire_release() throws ExecutionException, InterruptedException {
        CompletableFuture<?>[] results = new CompletableFuture[THREADS * 10];
        for (int i = 0 ; i < results.length ; i++) {
            results[i] = CompletableFuture.supplyAsync(ACQUIRE_RELEASE, executorService);
        }
        CompletableFuture.allOf(results).get();
    }
}
