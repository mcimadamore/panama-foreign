package jdk.incubator.foreign.pointer.api;

import jdk.incubator.foreign.*;

import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public class c_int {
    private final int value;

    public c_int(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static final MemoryLayout LAYOUT = MemoryLayouts.JAVA_INT;

    public static Pointer<c_int> allocate(long size) {
        MemorySegment segment = MemorySegment.allocateNative(MemoryLayout.ofSequence(size, LAYOUT));
        return new Pointer<c_int>(segment, LAYOUT) {
            @Override
            c_int getInternal(long index) {
                return c_int.get(segment, index);
            }

            @Override
            void setInternal(long index, c_int x) {
                c_int.set(segment, index, x);
            }
        };
    }

    static c_int get(MemorySegment segment, long index) {
        int value = MemoryAccess.getIntAtIndex(segment, index);
        return new c_int(value);
    }

    static void set(MemorySegment segment, long index, c_int c_int) {
        MemoryAccess.setIntAtIndex(segment, index, c_int.value());
    }
}
