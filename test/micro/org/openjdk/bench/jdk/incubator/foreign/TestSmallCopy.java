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

import jdk.incubator.foreign.MemoryLayouts;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import sun.misc.Unsafe;

import jdk.incubator.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class TestSmallCopy {
    static final Unsafe unsafe = Utils.unsafe;

    static final int ELEM_SIZE = 100;
    static final int CARRIER_SIZE = (int) MemoryLayouts.JAVA_BYTE.byteSize();
    static final int ALLOC_SIZE = ELEM_SIZE * CARRIER_SIZE;
    static final int UNSAFE_BYTE_OFFSET = unsafe.arrayBaseOffset(byte[].class);

    final long unsafe_addr = unsafe.allocateMemory(ALLOC_SIZE);
    final MemorySegment segment = MemorySegment.allocateNative(ALLOC_SIZE, ResourceScope.newConfinedScope());
    final ByteBuffer buffer = ByteBuffer.allocateDirect(ALLOC_SIZE).order(ByteOrder.nativeOrder());

    final byte[] bytes = new byte[ALLOC_SIZE];
    final MemorySegment bytesSegment = MemorySegment.ofArray(bytes);

    int srcOffset;
    int targetOffset;
    int nbytes;

    final Random random = new Random();

    @Setup
    public void setup() {
        srcOffset = random.nextInt(ELEM_SIZE / 4);
        targetOffset = random.nextInt(ELEM_SIZE / 4);
        nbytes = ELEM_SIZE / 2;
        for (int i = 0 ; i < bytes.length ; i++) {
            bytes[i] = (byte)i;
        }
    }

    @TearDown
    public void tearDown() {
        segment.scope().close();
        unsafe.invokeCleaner(buffer);
        unsafe.freeMemory(unsafe_addr);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void unsafe_small_copy() {
        unsafe.copyMemory(bytes, UNSAFE_BYTE_OFFSET + srcOffset, null, unsafe_addr + targetOffset, nbytes);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void segment_small_copy_slice() {
        segment.asSlice(srcOffset, nbytes).copyFrom(bytesSegment.asSlice(targetOffset, nbytes));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void buffer_small_copy() {
        buffer.put(targetOffset, bytes, srcOffset, nbytes);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void segment_small_copy_static() {
        MemorySegment.copy(bytesSegment, srcOffset, segment, targetOffset, nbytes);
    }
}
