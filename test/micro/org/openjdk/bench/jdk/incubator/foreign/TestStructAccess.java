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

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.SequenceLayout;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import sun.misc.Unsafe;

import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class TestStructAccess {

    static final Unsafe unsafe = Utils.unsafe;

    static final int SEQ_SIZE = 1_000_000;
    static final int ELEM_SIZE = SEQ_SIZE * 3;
    static final int VALUE_SIZE = 4;

    static final SequenceLayout LAYOUT = MemoryLayout.ofSequence(SEQ_SIZE,
            MemoryLayout.ofStruct(
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("key1"),
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("key2"),
                    MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).withName("value")
            )
    ).withName("KeyValues");


    static final VarHandle VH_int = MemoryLayout.ofSequence(JAVA_INT).path()
            .sequenceElement().varHandle(int.class);

    static final VarHandle VH_value = LAYOUT.path()
            .sequenceElement().groupElement("value").varHandle(int.class);

    static final MemoryLayout.Path VALUE_PATH = LAYOUT.path().sequenceElement().groupElement("value");
    static final MemoryLayout.Path KEY1_PATH = LAYOUT.path().sequenceElement().groupElement("key1");
    static final MemoryLayout.Path KEY2_PATH = LAYOUT.path().sequenceElement().groupElement("key2");

    MemorySegment segment;
    long unsafe_addr;

    ByteBuffer byteBuffer;

    @Setup
    public void setup() {
        unsafe_addr = unsafe.allocateMemory(LAYOUT.byteSize());
        for (int i = 0; i < ELEM_SIZE; i++) {
            unsafe.putInt(unsafe_addr + (i * VALUE_SIZE) , i);
        }
        segment = MemorySegment.allocateNative(LAYOUT);
        for (int i = 0; i < ELEM_SIZE; i++) {
            VH_int.set(segment, (long) i, i);
        }
        byteBuffer = ByteBuffer.allocateDirect((int)LAYOUT.byteSize()).order(ByteOrder.nativeOrder());
        for (int i = 0; i < ELEM_SIZE; i++) {
            byteBuffer.putInt(i * VALUE_SIZE , i);
        }

//        int res = 0;
//        for (int i = 0 ; i < SEQ_SIZE ; i++) {
//            res += MemoryAccess.getIntAtIndex(segment, KEY1_PATH, i);
//        }
//        System.err.println(res);
//        res = 0;
//        for (int i = 0 ; i < SEQ_SIZE ; i++) {
//            res += MemoryAccess.getIntAtIndex(segment, KEY2_PATH, i);
//        }
//        System.err.println(res);
//        res = 0;
//        for (int i = 0 ; i < SEQ_SIZE ; i++) {
//            res += MemoryAccess.getIntAtIndex(segment, VALUE_PATH, i);
//        }
//        System.err.println(res);
    }

    @TearDown
    public void tearDown() {
        segment.close();
        unsafe.invokeCleaner(byteBuffer);
        unsafe.freeMemory(unsafe_addr);
    }

    @Benchmark
    public int unsafe_loop() {
        int res = 0;
        for (int i = 0; i < SEQ_SIZE; i ++) {
            res += unsafe.getInt(unsafe_addr + ((i * 2 + 1) * VALUE_SIZE));
        }
        return res;
    }

    @Benchmark
    public int segment_loop() {
        int sum = 0;
        for (int i = 0; i < SEQ_SIZE; i++) {
            sum += (int) VH_value.get(segment, (long) i);
        }
        return sum;
    }

    @Benchmark
    public int segment_loop_path() {
        int sum = 0;
        for (int i = 0; i < SEQ_SIZE; i++) {
            sum += MemoryAccess.getIntAtIndex(segment, VALUE_PATH, i);
        }
        return sum;
    }

    @Benchmark
    public int BB_loop() {
        int sum = 0;
        ByteBuffer bb = byteBuffer;
        for (int i = 0; i < SEQ_SIZE; i++) {
            sum += bb.getInt(((i * 2 + 1) * VALUE_SIZE));
        }
        return sum;
    }

}
