package jdk.incubator.foreign.pointer.api;

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

public abstract class Pointer<X> {
    private final MemorySegment segment;
    private final MemoryLayout type;

    Pointer(MemorySegment segment, MemoryLayout layout) {
        this.segment = segment;
        this.type = layout;
    }

    abstract X getInternal(long index);

    abstract void setInternal(long index, X x);

    @SuppressWarnings("unchecked")
    public X get() {
        return getInternal(0);
    }
    @SuppressWarnings("unchecked")
    public X get(long index) {
        return getInternal(index);
    }
    public void set(X x) {
        setInternal(0, x);
    }
    public void set(long index, X x) {
        setInternal(index, x);
    }

    public MemoryLayout type() {
        return type;
    }

    public MemorySegment segment() {
        return segment;
    }

    public long toRawLongValue() {
        return segment.address().toRawLongValue();
    }

//    static class PointerType<X> extends ForeignType<Pointer<X>> {
//
//        static final VarHandle handle = MemoryHandles.varHandle(long.class, ByteOrder.nativeOrder());
//
//        private final ForeignType<X> pointee;
//        private final MemoryLayout layout;
//
//        PointerType(MemoryLayout layout, ForeignType<X> pointee) {
//            this.layout = layout;
//            this.pointee = pointee;
//        }
//
//        @Override
//        MemoryLayout layout() {
//            return layout;
//        }
//
//        @Override
//        Pointer<X> get(MemorySegment base, long offset) {
//            long addr = (long)handle.get(base, offset);
//            return new Pointer<>(addr, pointee);
//        }
//
//        @Override
//        void set(MemorySegment base, long offset, Pointer<X> xPointer) {
//            handle.set(base, offset, xPointer.addr().toRawLongValue());
//        }
//    };
}
