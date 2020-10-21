package jdk.incubator.foreign.pointer.api;

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;

public abstract class ForeignType<X> {
    public abstract MemoryLayout layout();

    public long byteSize() { return layout().byteSize(); }

    public final ForeignType<Pointer<X>> pointerType() {
        return new Pointer.PointerType<>(MemoryLayouts.JAVA_LONG, this);
    }

    public abstract X get(MemorySegment address, long offset);
    public abstract void set(MemorySegment address, long offset, X x);

    public Pointer<X> allocate() {  // one
        MemorySegment segment = MemorySegment.allocateNative(layout());
        return new Pointer<>(segment.address(), this);
    }

    public Pointer<X> allocate(long size) { // many
        MemorySegment segment = MemorySegment.allocateNative(MemoryLayout.ofSequence(size, layout()));
        return new Pointer<>(segment.address(), this);
    }
}
