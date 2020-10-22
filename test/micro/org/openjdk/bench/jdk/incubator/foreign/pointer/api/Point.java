package jdk.incubator.foreign.pointer.api;

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

public class Point {
    
    private final MemorySegment segment;

    Point(MemorySegment segment) {
        this.segment = segment;
    }

    public static final MemoryLayout LAYOUT = MemoryLayout.ofStruct(
            c_int.LAYOUT.withName("x"),
            c_int.LAYOUT.withName("y"));

    static final long X_OFFSET = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("x"));
    static final long Y_OFFSET = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("y"));

    public c_int x$get() {
        return c_int.get(segment, 0);
    }

    public c_int y$get() {
        return c_int.get(segment, 1);
    }

    public void x$set(c_int x) {
        c_int.set(segment, 0, x);
    }

    public void y$set(c_int y) {
        c_int.set(segment, 1, y);
    }

    public static Pointer<Point> allocate(long size) {
        MemorySegment segment = MemorySegment.allocateNative(MemoryLayout.ofSequence(size, LAYOUT));
        return new Pointer<Point>(segment, LAYOUT) {
            @Override
            Point getInternal(long index) {
                MemorySegment slice = pointSlice(segment, index * LAYOUT.byteSize());
                return new Point(slice);
            }

            @Override
            void setInternal(long index, Point point) {
                pointSlice(segment, index * LAYOUT.byteSize()).copyFrom(point.segment);
            }

            MemorySegment pointSlice(MemorySegment base, long offset) {
                return base.asSlice(offset, LAYOUT.byteSize());
            }
        };
    }
}
