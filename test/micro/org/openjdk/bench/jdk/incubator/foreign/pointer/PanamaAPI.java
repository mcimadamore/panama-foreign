package jdk.incubator.foreign.pointer;

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.pointer.api.Point;
import jdk.incubator.foreign.pointer.api.Pointer;
import jdk.incubator.foreign.pointer.api.c_int;
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(3)
public class PanamaAPI {

    static final Unsafe unsafe = org.openjdk.bench.jdk.incubator.foreign.Utils.unsafe;

    static final int ELEM_SIZE = 100_000;
    static final MemorySegment EVERYTHING = MemorySegment.ofNativeRestricted();

    Pointer<c_int> int_ptr;
    Pointer<Pointer<c_int>> int_ptr_ptr;
    Pointer<Point> point_ptr;

    @Setup
    public void setup() {
        int_ptr = c_int.TYPE.allocate(ELEM_SIZE);
        for (int i = 0 ; i < ELEM_SIZE ; i++) {
            int_ptr.set(i, new c_int(i));
        }

        int_ptr_ptr = c_int.TYPE.pointerType().allocate(ELEM_SIZE);
        for (int i = 0 ; i < ELEM_SIZE ; i++) {
            int_ptr_ptr.set(i, new Pointer<>(MemoryAddress.ofLong(i), c_int.TYPE));
        }

        point_ptr = Point.TYPE.allocate(ELEM_SIZE);
        for (int i = 0 ; i < ELEM_SIZE ; i++) {
            Point point = point_ptr.get(i);
            point.x$set(new c_int(i));
            point.y$set(new c_int(i + 1));
        }
    }

    @TearDown
    public void tearDown() {
        unsafe.freeMemory(int_ptr.toRawLongValue());
        unsafe.freeMemory(int_ptr_ptr.toRawLongValue());
        unsafe.freeMemory(point_ptr.toRawLongValue());
    }

    @Benchmark
    public int intPtr_low() {
        int sum = 0;
        MemorySegment base = int_ptr.segment(ELEM_SIZE);
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += (int)MemoryAccess.getIntAtIndex(base, i);
        }
        return sum;
    }

    @Benchmark
    public int intPtr_high() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += int_ptr.get(i).value();
        }
        return sum;
    }

//    @Benchmark
//    public int intPtr_mid1() {
//        int sum = 0;
//        MemorySegment base = int_ptr.segment(ELEM_SIZE);
//        for (int i = 0; i < ELEM_SIZE; i++) {
//            sum += c_int.TYPE.get(base, i * 4).value();
//        }
//        return sum;
//    }
//
//    @Benchmark
//    public int intPtr_mid2() {
//        int sum = 0;
//        MemorySegment base = int_ptr.segment(ELEM_SIZE);
//        for (int i = 0; i < ELEM_SIZE; i++) {
//            sum += c_int.INT_TYPE.get(base, i * 4).value();
//        }
//        return sum;
//    }

    @Benchmark
    public int intPtrPtr_low() {
        int sum = 0;
        MemorySegment base = int_ptr_ptr.segment(ELEM_SIZE);
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += MemoryAccess.getLongAtIndex(base, i);
        }
        return sum;
    }

    @Benchmark
    public int intPtrPtr_high() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += (int) int_ptr_ptr.get(i).toRawLongValue();
        }
        return sum;
    }

    @Benchmark
    public int pointPtr_low() {
        int sum = 0;
        long base = point_ptr.toRawLongValue();
        int size = (int) Point.TYPE.layout().byteSize();
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += (int)MemoryAccess.getIntAtOffset(EVERYTHING, base + (i * size));
        }
        return sum;
    }

    @Benchmark
    public int pointPtr_high() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += point_ptr.get(i).x$get().value();
        }
        return sum;
    }
}
