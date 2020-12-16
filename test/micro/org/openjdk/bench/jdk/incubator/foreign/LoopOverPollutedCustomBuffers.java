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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jdk.incubator.foreign.MemoryAccess;
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
import org.openjdk.jmh.annotations.Warmup;

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

    private CustomFloatBuffer nativeBuffer;

    @Param({ "0", "1", "2" })
    int pollute;

    @Setup
    public void setUp() {
        int numPixels = WIDTH * HEIGHT;
        nativeBuffer = CustomFloatBuffer.of(NUM_ELEMS);
        CustomFloatBuffer heapBufferFloats = CustomFloatBuffer.of(new float[numPixels]);
        CustomFloatBuffer heapBufferInts = CustomFloatBuffer.of(new float[numPixels]);
        float f = 0;
        for (int i = 0; i < WIDTH; ++i) {
            for (int j = 0; j < HEIGHT; ++j) {
                nativeBuffer.setFloat(i * j, i + j);
                if (pollute > 0) {
                    f += heapBufferFloats.getFloat(i * j);
                }
                if (pollute > 1) {
                    f += heapBufferInts.getFloat(i * j);
                }
            }
        }
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
    public float readAllValuesLinear() {
        float v = 0;
        for (int i = 0; i < NUM_ELEMS; ++i) {
            v += nativeBuffer.getFloat(i);
        }
        return v;
    }

    static class CustomFloatBuffer {
        final MemorySegment segment;

        CustomFloatBuffer(MemorySegment segment) {
            this.segment = segment;
        }

        float getFloat(long index) {
            return MemoryAccess.getFloatAtIndex(segment, index);
        }

        void setFloat(long index, float value) {
            MemoryAccess.setFloatAtIndex(segment, index, value);
        }

        static CustomFloatBuffer of(int size) {
            return new CustomFloatBuffer(MemorySegment.allocateNative(4 * size, 4));
        }

        static CustomFloatBuffer of(float[] array) {
            return new CustomFloatBuffer(MemorySegment.ofArray(array));
        }

        static CustomFloatBuffer of(int[] array) {
            return new CustomFloatBuffer(MemorySegment.ofArray(array));
        }
    }
}