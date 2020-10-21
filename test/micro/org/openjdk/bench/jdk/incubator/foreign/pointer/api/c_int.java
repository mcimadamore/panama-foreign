package jdk.incubator.foreign.pointer.api;

import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;

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

    static final VarHandle handle = MemoryHandles.varHandle(int.class, ByteOrder.nativeOrder());

    public static class IntType extends ForeignType<c_int> {
        @Override
        public MemoryLayout layout() {
            return MemoryLayouts.JAVA_INT;
        }

        @Override
        public c_int get(MemorySegment base, long offset) {
            int value = (int)handle.get(base, offset);
            return new c_int(value);
        }

        @Override
        public void set(MemorySegment base, long offset, c_int c_int) {
            handle.set(base, offset, c_int.value());
        }
    }

    public static final ForeignType<c_int> TYPE = new IntType();

    public static final IntType INT_TYPE = new IntType();
}
