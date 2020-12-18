/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntFunction;

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import sun.misc.Unsafe;

@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class LoopOverPollutedCustomBuffers {

    static final int WIDTH = 786432;
    static final int HEIGHT = 3;
    static final int NUM_ELEMS = WIDTH * HEIGHT;

    private CustomFloatBuffer nativeBuffer, heapBuffer;

    @Param({ "false", "true" })
    boolean pollute;

    @Param({ "NIO", "SEGMENT_VH", "SEGMENT_DIRECT" })
    BufferKind bufferKind;

    public enum BufferKind {
        NIO(CustomFloatBufferNIO::of, CustomFloatBufferNIO::of),
        SEGMENT_VH(CustomFloatBufferSegmentVH::of, CustomFloatBufferSegmentVH::of),
        SEGMENT_DIRECT(CustomFloatBufferSegmentDirect::of, CustomFloatBufferSegmentDirect::of);

        private final IntFunction<CustomFloatBuffer> directFactory;
        private final Function<byte[], CustomFloatBuffer> heapViewFactory;

        BufferKind(IntFunction<CustomFloatBuffer> directFactory, Function<byte[], CustomFloatBuffer> heapViewFactory) {
            this.directFactory = directFactory;
            this.heapViewFactory = heapViewFactory;
        }

        CustomFloatBuffer makeDirect(int size) {
            return directFactory.apply(size);
        }

        CustomFloatBuffer makeHeapView(byte[] arr) {
            return heapViewFactory.apply(arr);
        }
    }

    static final Unsafe unsafe = Utils.unsafe;

    @Setup
    public void setUp() {
        int numPixels = WIDTH * HEIGHT;
        nativeBuffer = bufferKind.makeDirect(NUM_ELEMS);
        heapBuffer = bufferKind.makeHeapView(new byte[numPixels * 4]);
        float f = 0;
        for (int i = 0; i < WIDTH; ++i) {
            for (int j = 0; j < HEIGHT; ++j) {
                nativeBuffer.setFloat(i * j, i + j);
                if (pollute) {
                    f += heapBuffer.getFloat(i * j);
                }
            }
        }
    }

    @TearDown
    public void tearDown() {
        nativeBuffer.close();
    }

    @Benchmark
    public float readAllValuesMatrix() {
        float v = 0;
        for (int i = 0; i < WIDTH; ++i) {
            for (int j = 0; j < HEIGHT; ++j) {
                v += nativeBuffer.getFloat(i * j);
            }
        }
        return v;
    }

    @Benchmark
    public float readAllValuesMatrixHeap() {
        float v = 0;
        for (int i = 0; i < WIDTH; ++i) {
            for (int j = 0; j < HEIGHT; ++j) {
                v += heapBuffer.getFloat(i * j);
            }
        }
        return v;
    }

    @Benchmark
    public float readAllValuesLinear() {
        float v = 0;
        for (int i = 0; i < NUM_ELEMS; ++i) {
            v += nativeBuffer.getFloat(i);
        }
        return v;
    }

    @Benchmark
    public float readAllValuesLinearHeap() {
        float v = 0;
        for (int i = 0; i < NUM_ELEMS; ++i) {
            v += heapBuffer.getFloat(i);
        }
        return v;
    }

    interface CustomFloatBuffer {
        float getFloat(long index);
        void setFloat(long index, float value);
        void close();
    }

    static class CustomFloatBufferSegmentVH implements CustomFloatBuffer {
        final MemorySegment segment;

        static final VarHandle floatHandle = MemoryLayout.ofSequence(MemoryLayouts.JAVA_FLOAT).varHandle(float.class, MemoryLayout.PathElement.sequenceElement());

        CustomFloatBufferSegmentVH(MemorySegment segment) {
            this.segment = segment;
        }

        @Override
        public float getFloat(long index) {
            return (float)floatHandle.get(segment, index);
        }

        @Override
        public void setFloat(long index, float value) {
            floatHandle.set(segment, index, value);
        }

        static CustomFloatBuffer of(int size) {
            return new CustomFloatBufferSegmentVH(MemorySegment.allocateNative(4 * size, 4));
        }

        static CustomFloatBuffer of(byte[] array) {
            return new CustomFloatBufferSegmentVH(MemorySegment.ofArray(array));
        }

        public void close() {
            segment.close();
        }
    }

    static class CustomFloatBufferSegmentDirect implements CustomFloatBuffer {
        final MemorySegment segment;

        CustomFloatBufferSegmentDirect(MemorySegment segment) {
            this.segment = segment;
        }

        @Override
        public float getFloat(long index) {
            return segment.getFloat(index * 4);
        }

        @Override
        public void setFloat(long index, float value) {
            segment.setFloat(index * 4, value);
        }

        static CustomFloatBuffer of(int size) {
            return new CustomFloatBufferSegmentDirect(MemorySegment.allocateNative(4 * size, 4));
        }

        static CustomFloatBuffer of(byte[] array) {
            return new CustomFloatBufferSegmentDirect(MemorySegment.ofArray(array));
        }

        public void close() {
            segment.close();
        }
    }

    static class CustomFloatBufferNIO implements CustomFloatBuffer {
        final FloatBuffer fb;
        final ByteBuffer bb;

        CustomFloatBufferNIO(ByteBuffer bb, FloatBuffer fb) {
            this.bb = bb;
            this.fb = fb;
        }

        @Override
        public float getFloat(long index) {
            return fb.get((int)index);
        }

        @Override
        public void setFloat(long index, float value) {
            fb.put((int)index, value);
        }

        static CustomFloatBuffer of(int size) {
            ByteBuffer bb = ByteBuffer.allocateDirect(4 * size);
            return new CustomFloatBufferNIO(bb, bb.order(ByteOrder.nativeOrder()).asFloatBuffer());
        }

        static CustomFloatBuffer of(byte[] array) {
            ByteBuffer bb = ByteBuffer.wrap(array);
            return new CustomFloatBufferNIO(bb, bb.order(ByteOrder.nativeOrder()).asFloatBuffer());
        }

        public void close() {
            unsafe.invokeCleaner(bb);
        }
    }
}
