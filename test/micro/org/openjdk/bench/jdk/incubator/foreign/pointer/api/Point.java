package jdk.incubator.foreign.pointer.api;

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

public class Point {
    
    private final MemorySegment segment;

    Point(MemorySegment segment) {
        this.segment = segment;
    }

    static final MemoryLayout LAYOUT = MemoryLayout.ofStruct(
            c_int.TYPE.layout().withName("x"),
            c_int.TYPE.layout().withName("y"));
    
    static final long X_OFFSET = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("x"));
    static final long Y_OFFSET = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("y"));
    
    public c_int x$get() {
        return c_int.TYPE.get(segment, X_OFFSET);
    }

    public c_int y$get() {
        return c_int.TYPE.get(segment, Y_OFFSET);
    }

    public void x$set(c_int x) {
        c_int.TYPE.set(segment, X_OFFSET, x);
    }

    public void y$set(c_int y) {
        c_int.TYPE.set(segment, Y_OFFSET, y);
    }

    static class PointType extends ForeignType<Point> {
        @Override
        public MemoryLayout layout() {
            return LAYOUT;
        }

        @Override
        public Point get(MemorySegment base, long offset) {
            MemorySegment segment = pointSlice(base, offset);
            return new Point(segment);
        }

        @Override
        public void set(MemorySegment address, long offset, Point p) {
            pointSlice(address, offset).copyFrom(p.segment);
        }

        static MemorySegment pointSlice(MemorySegment base, long offset) {
            return base.asSlice(offset, LAYOUT.byteSize());
        }
    };

    public static ForeignType<Point> TYPE = new PointType();
}
