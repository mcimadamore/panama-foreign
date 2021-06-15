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
import sun.misc.Unsafe;
import jdk.incubator.foreign.ResourceScope;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import static org.openjdk.jmh.annotations.CompilerControl.Mode.*;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import sun.misc.Unsafe;

import jdk.incubator.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class TestSmallCopy {
    static final Unsafe unsafe = Utils.unsafe;

    static final int ELEM_SIZE = 100;
    static final int CARRIER_SIZE = (int)JAVA_INT.byteSize();
    static final int ALLOC_SIZE = ELEM_SIZE * CARRIER_SIZE;

    static final long unsafe_addr = unsafe.allocateMemory(ALLOC_SIZE);
    static final MemorySegment segment = MemorySegment.allocateNative(ALLOC_SIZE, ResourceScope.newConfinedScope());

    static final int[] bytes = new int[ELEM_SIZE];
    static final MemorySegment bytesSegment = MemorySegment.ofArray(bytes);
    static final int UNSAFE_INT_OFFSET = unsafe.arrayBaseOffset(int[].class);

    int srcOffset;
    int targetOffset;
    int nbytes;

    static final Random random = new Random();

    static {
        for (int i = 0 ; i < bytes.length ; i++) {
            bytes[i] = i;
        }
    }

    @Setup//(Level.Iteration)
    public void setup() {
        srcOffset = random.nextInt(ELEM_SIZE / 4);
        targetOffset = random.nextInt(ELEM_SIZE / 4);
        nbytes = ELEM_SIZE / 4 + random.nextInt(ELEM_SIZE / 4);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void unsafe_small_copy() {
        unsafe.copyMemory(bytes, UNSAFE_INT_OFFSET + srcOffset, null, unsafe_addr + targetOffset, nbytes);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void unsafe_small_copy_address() {
        unsafe.copyMemory(bytes, UNSAFE_INT_OFFSET + srcOffset, null, segment.address().toRawLongValue() + targetOffset, nbytes);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void segment_small_copy() {
        segment.asSlice(srcOffset, nbytes).copyFrom(bytesSegment.asSlice(targetOffset, nbytes));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void segment_small_copy_static() {
        MemorySegment.copy(bytesSegment, srcOffset, segment, targetOffset, nbytes);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void segment_small_copy_fresh() {
        segment.asSlice(srcOffset, nbytes).copyFrom(MemorySegment.ofArray(bytes).asSlice(targetOffset, nbytes));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void segment_small_copy_fresh_reverse() {
        MemorySegment.ofArray(bytes).asSlice(srcOffset, nbytes).copyFrom(segment.asSlice(targetOffset, nbytes));
    }
}
