package jdk.incubator.foreign.pointer.api;

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public class Pointer<X> {
    private final long addr;
    private final ForeignType<X> type;

    private final static MemorySegment EVERYTHING = MemorySegment.ofNativeRestricted();

    public Pointer(MemoryAddress addr, ForeignType<X> type) {
        this(addr.toRawLongValue(), type);
    }

    private Pointer(long addr, ForeignType<X> type) {
        this.addr = addr;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public X get() {
        return type.get(EVERYTHING, addr);
    }
    @SuppressWarnings("unchecked")
    public X get(long index) {
        return type.get(EVERYTHING, addr + (type.byteSize() * index));
    }
    public void set(X x) {
        type.set(EVERYTHING, addr, x);
    }
    public void set(long index, X x) {
        type.set(EVERYTHING, addr + (type.byteSize() * index), x);
    }

    public ForeignType<X> type() {
        return type;
    }

    public MemoryAddress addr() {
        return MemoryAddress.ofLong(addr);
    }

    public long toRawLongValue() {
        return addr;
    }

    public MemorySegment segment(long nelems) {
        return MemorySegment.ofNativeRestricted().asSlice(addr, nelems * type.layout().byteSize());
    }

    static class PointerType<X> extends ForeignType<Pointer<X>> {

        static final VarHandle handle = MemoryHandles.varHandle(long.class, ByteOrder.nativeOrder());

        private final ForeignType<X> pointee;
        private final MemoryLayout layout;

        PointerType(MemoryLayout layout, ForeignType<X> pointee) {
            this.layout = layout;
            this.pointee = pointee;
        }

        @Override
        public MemoryLayout layout() {
            return layout;
        }

        @Override
        public Pointer<X> get(MemorySegment base, long offset) {
            long addr = (long)handle.get(base, offset);
            return new Pointer<>(addr, pointee);
        }

        @Override
        public void set(MemorySegment base, long offset, Pointer<X> xPointer) {
            handle.set(base, offset, xPointer.addr().toRawLongValue());
        }
    };
}
