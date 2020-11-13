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

package jdk.internal.foreign;

import jdk.incubator.foreign.*;
import jdk.internal.access.JavaNioAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.access.foreign.MemorySegmentProxy;
import jdk.internal.access.foreign.UnmapperProxy;
import jdk.internal.misc.ScopedMemoryAccess;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import jdk.internal.util.ArraysSupport;
import jdk.internal.vm.annotation.ForceInline;
import sun.nio.ch.FileChannelImpl;
import sun.security.action.GetBooleanAction;
import sun.security.action.GetPropertyAction;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * This class provides an immutable implementation for the {@code MemorySegment} interface. This class contains information
 * about the segment's spatial and temporal bounds; each memory segment implementation is associated with an owner thread which is set at creation time.
 * Access to certain sensitive operations on the memory segment will fail with {@code IllegalStateException} if the
 * segment is either in an invalid state (e.g. it has already been closed) or if access occurs from a thread other
 * than the owner thread. See {@link MemoryScope} for more details on management of temporal bounds.
 */
public class MemorySegmentImpl implements MemorySegment, MemorySegmentProxy {

    private static final ScopedMemoryAccess SCOPED_MEMORY_ACCESS = ScopedMemoryAccess.getScopedMemoryAccess();

    private static final boolean enableSmallSegments =
            Boolean.parseBoolean(GetPropertyAction.privilegedGetProperty("jdk.incubator.foreign.SmallSegments", "true"));

    final static int FIRST_RESERVED_FLAG = 1 << 16; // upper 16 bits are reserved
    final static int SMALL = FIRST_RESERVED_FLAG;

    final static byte NO_KIND = 0;
    final static byte BYTE_KIND = 1;
    final static byte CHAR_KIND = 2;
    final static byte SHORT_KIND = 3;
    final static byte INT_KIND = 4;
    final static byte FLOAT_KIND = 5;
    final static byte LONG_KIND = 6;
    final static byte DOUBLE_KIND = 7;


    final static long NONCE = new Random().nextLong();

    final static JavaNioAccess nioAccess = SharedSecrets.getJavaNioAccess();

    final long length;
    final int mask;
    final MemoryScope scope;
    final long min;
    final Object base;
    final UnmapperProxy unmapper;
    final byte kind;

    @ForceInline
    MemorySegmentImpl(byte kind, long min, Object base, UnmapperProxy unmapper, long length, int mask, MemoryScope scope) {
        this.length = length;
        this.mask = mask;
        this.scope = scope;
        this.min = min;
        this.base = base;
        this.unmapper = unmapper;
        this.kind = kind;
    }

    final Object base() {
        return switch (kind) {
            case NO_KIND -> null;
            case BYTE_KIND -> (byte[])base;
            case CHAR_KIND -> (char[])base;
            case SHORT_KIND -> (short[])base;
            case INT_KIND -> (int[])base;
            case FLOAT_KIND -> (float[])base;
            case LONG_KIND -> (long[])base;
            case DOUBLE_KIND -> (double[])base;
            default -> throw new AssertionError();
        };
    }

    @Override
    public boolean isMapped() {
        return unmapper != null;
    }

    static int defaultAccessModes(long size) {
        return (enableSmallSegments && size < Integer.MAX_VALUE) ?
                ALL_ACCESS | SMALL :
                ALL_ACCESS;
    }

    @Override
    public MemorySegmentImpl asSlice(long offset, long newSize) {
        checkBounds(offset, newSize);
        return asSliceNoCheck(offset, newSize);
    }

    @Override
    public MemorySegmentImpl asSlice(long offset) {
        checkBounds(offset, 0);
        return asSliceNoCheck(offset, length - offset);
    }

    private MemorySegmentImpl asSliceNoCheck(long offset, long newSize) {
        return new MemorySegmentImpl(kind, min + offset, base, unmapper, newSize, mask, scope);
    }

    @Override
    public Spliterator<MemorySegment> spliterator(SequenceLayout sequenceLayout) {
        checkValidState();
        if (sequenceLayout.byteSize() != byteSize()) {
            throw new IllegalArgumentException();
        }
        return new SegmentSplitter(sequenceLayout.elementLayout().byteSize(), sequenceLayout.elementCount().getAsLong(),
                withAccessModes(accessModes() & ~CLOSE));
    }

    @Override
    public final MemorySegment fill(byte value){
        checkAccess(0, length, false);
        SCOPED_MEMORY_ACCESS.setMemory(scope, base(), min, length, value);
        return this;
    }

    public void copyFrom(MemorySegment src) {
        MemorySegmentImpl that = (MemorySegmentImpl)src;
        long size = that.byteSize();
        checkAccess(0, size, false);
        that.checkAccess(0, size, true);
        SCOPED_MEMORY_ACCESS.copyMemory(scope, that.scope,
                that.base(), that.min,
                base(), min, size);
    }

    private final static VarHandle BYTE_HANDLE = MemoryLayout.ofSequence(MemoryLayouts.JAVA_BYTE)
            .varHandle(byte.class, MemoryLayout.PathElement.sequenceElement());

    @Override
    public long mismatch(MemorySegment other) {
        MemorySegmentImpl that = (MemorySegmentImpl)other;
        final long thisSize = this.byteSize();
        final long thatSize = that.byteSize();
        final long length = Math.min(thisSize, thatSize);
        this.checkAccess(0, length, true);
        that.checkAccess(0, length, true);
        if (this == other) {
            checkValidState();
            return -1;
        }

        long i = 0;
        if (length > 7) {
            if ((byte) BYTE_HANDLE.get(this, 0) != (byte) BYTE_HANDLE.get(that, 0)) {
                return 0;
            }
            i = vectorizedMismatchLargeForBytes(scope, that.scope,
                    this.base(), this.min,
                    that.base(), that.min,
                    length);
            if (i >= 0) {
                return i;
            }
            long remaining = ~i;
            assert remaining < 8 : "remaining greater than 7: " + remaining;
            i = length - remaining;
        }
        for (; i < length; i++) {
            if ((byte) BYTE_HANDLE.get(this, i) != (byte) BYTE_HANDLE.get(that, i)) {
                return i;
            }
        }
        return thisSize != thatSize ? length : -1;
    }

    /**
     * Mismatch over long lengths.
     */
    private static long vectorizedMismatchLargeForBytes(MemoryScope aScope, MemoryScope bScope,
                                                        Object a, long aOffset,
                                                        Object b, long bOffset,
                                                        long length) {
        long off = 0;
        long remaining = length;
        int i, size;
        boolean lastSubRange = false;
        while (remaining > 7 && !lastSubRange) {
            if (remaining > Integer.MAX_VALUE) {
                size = Integer.MAX_VALUE;
            } else {
                size = (int) remaining;
                lastSubRange = true;
            }
            i = SCOPED_MEMORY_ACCESS.vectorizedMismatch(aScope, bScope,
                    a, aOffset + off,
                    b, bOffset + off,
                    size, ArraysSupport.LOG2_ARRAY_BYTE_INDEX_SCALE);
            if (i >= 0)
                return off + i;

            i = size - ~i;
            off += i;
            remaining -= i;
        }
        return ~remaining;
    }

    @Override
    @ForceInline
    public final MemoryAddress address() {
        checkValidState();
        return new MemoryAddressImpl(base(), min);
    }

    @Override
    public final ByteBuffer asByteBuffer() {
        if (!isSet(READ)) {
            throw unsupportedAccessMode(READ);
        }
        checkArraySize("ByteBuffer", 1);
        ByteBuffer _bb;
        if (base != null) {
            //heap
            if (!(base instanceof byte[])) {
                throw new UnsupportedOperationException("Not an address to an heap-allocated byte array");
            }
            JavaNioAccess nioAccess = SharedSecrets.getJavaNioAccess();
            _bb = nioAccess.newHeapByteBuffer((byte[]) base, (int)min - BYTE_ARR_BASE, (int) byteSize(), this);
        } else if (unmapper == null) {
            // direct
            _bb = nioAccess.newDirectByteBuffer(min, (int) this.length, null, this);
        } else {
            // mapped
            _bb = nioAccess.newMappedByteBuffer(unmapper, min, (int)length, null, this);
        }
        if (!isSet(WRITE)) {
            //scope is IMMUTABLE - obtain a RO byte buffer
            _bb = _bb.asReadOnlyBuffer();
        }
        return _bb;
    }

    @Override
    public final int accessModes() {
        return mask & ALL_ACCESS;
    }

    @Override
    public final long byteSize() {
        return length;
    }

    @Override
    public final boolean isAlive() {
        return scope.isAlive();
    }

    @Override
    public Thread ownerThread() {
        return scope.ownerThread();
    }

    @Override
    public MemorySegmentImpl withAccessModes(int accessModes) {
        checkAccessModes(accessModes);
        if ((~accessModes() & accessModes) != 0) {
            throw new IllegalArgumentException("Cannot acquire more access modes");
        }
        return new MemorySegmentImpl(kind, min, base, unmapper, length, (mask & ~ALL_ACCESS) | accessModes, scope);
    }

    @Override
    public boolean hasAccessModes(int accessModes) {
        checkAccessModes(accessModes);
        return (accessModes() & accessModes) == accessModes;
    }

    private void checkAccessModes(int accessModes) {
        if ((accessModes & ~ALL_ACCESS) != 0) {
            throw new IllegalArgumentException("Invalid access modes");
        }
    }

    public MemorySegment handoff(Thread thread) {
        Objects.requireNonNull(thread);
        checkValidState();
        if (!isSet(HANDOFF)) {
            throw unsupportedAccessMode(HANDOFF);
        }
        try {
            return new MemorySegmentImpl(kind, min, base, unmapper, length, mask, scope.confineTo(thread));
        } finally {
            //flush read/writes to segment memory before returning the new segment
            VarHandle.fullFence();
        }
    }

    @Override
    public MemorySegment share() {
        checkValidState();
        if (!isSet(SHARE)) {
            throw unsupportedAccessMode(SHARE);
        }
        try {
            return new MemorySegmentImpl(kind, min, base, unmapper, length, mask, scope.share());
        } finally {
            //flush read/writes to segment memory before returning the new segment
            VarHandle.fullFence();
        }
    }

    @Override
    public MemorySegment registerCleaner(Cleaner cleaner) {
        Objects.requireNonNull(cleaner);
        checkValidState();
        if (!isSet(CLOSE)) {
            throw unsupportedAccessMode(CLOSE);
        }
        return new MemorySegmentImpl(kind, min, base, unmapper, length, mask, scope.cleanable(cleaner));
    }

    @Override
    public final void close() {
        checkValidState();
        if (!isSet(CLOSE)) {
            throw unsupportedAccessMode(CLOSE);
        }
        scope.close();
    }

    @Override
    public final byte[] toByteArray() {
        return toArray(byte[].class, 1, byte[]::new, MemorySegment::ofArray);
    }

    @Override
    public final short[] toShortArray() {
        return toArray(short[].class, 2, short[]::new, MemorySegment::ofArray);
    }

    @Override
    public final char[] toCharArray() {
        return toArray(char[].class, 2, char[]::new, MemorySegment::ofArray);
    }

    @Override
    public final int[] toIntArray() {
        return toArray(int[].class, 4, int[]::new, MemorySegment::ofArray);
    }

    @Override
    public final float[] toFloatArray() {
        return toArray(float[].class, 4, float[]::new, MemorySegment::ofArray);
    }

    @Override
    public final long[] toLongArray() {
        return toArray(long[].class, 8, long[]::new, MemorySegment::ofArray);
    }

    @Override
    public final double[] toDoubleArray() {
        return toArray(double[].class, 8, double[]::new, MemorySegment::ofArray);
    }

    private <Z> Z toArray(Class<Z> arrayClass, int elemSize, IntFunction<Z> arrayFactory, Function<Z, MemorySegment> segmentFactory) {
        int size = checkArraySize(arrayClass.getSimpleName(), elemSize);
        Z arr = arrayFactory.apply(size);
        MemorySegment arrSegment = segmentFactory.apply(arr);
        arrSegment.copyFrom(this);
        return arr;
    }

    @Override
    public boolean isSmall() {
        return isSet(SMALL);
    }

    @Override
    public void checkAccess(long offset, long length, boolean readOnly) {
        if (!readOnly && !isSet(WRITE)) {
            throw unsupportedAccessMode(WRITE);
        } else if (readOnly && !isSet(READ)) {
            throw unsupportedAccessMode(READ);
        }
        checkBounds(offset, length);
    }

    private void checkAccessAndScope(long offset, long length, boolean readOnly) {
        checkValidState();
        checkAccess(offset, length, readOnly);
    }

    private void checkValidState() {
        try {
            scope.checkValidState();
        } catch (ScopedMemoryAccess.Scope.ScopedAccessError ex) {
            throw new IllegalStateException("This segment is already closed");
        }
    }

    @Override
    public long unsafeGetOffset() {
        return min;
    }

    @Override
    public Object unsafeGetBase() {
        return base();
    }

    // Helper methods

    private boolean isSet(int mask) {
        return (this.mask & mask) != 0;
    }

    private int checkArraySize(String typeName, int elemSize) {
        if (length % elemSize != 0) {
            throw new UnsupportedOperationException(String.format("Segment size is not a multiple of %d. Size: %d", elemSize, length));
        }
        long arraySize = length / elemSize;
        if (arraySize > (Integer.MAX_VALUE - 8)) { //conservative check
            throw new UnsupportedOperationException(String.format("Segment is too large to wrap as %s. Size: %d", typeName, length));
        }
        return (int)arraySize;
    }

    private void checkBounds(long offset, long length) {
        if (isSmall()) {
            checkBoundsSmall((int)offset, (int)length);
        } else {
            if (length < 0 ||
                    offset < 0 ||
                    offset > this.length - length) { // careful of overflow
                throw outOfBoundException(offset, length);
            }
        }
    }

    @Override
    public MemoryScope scope() {
        return scope;
    }

    private void checkBoundsSmall(int offset, int length) {
        if (length < 0 ||
                offset < 0 ||
                offset > (int)this.length - length) { // careful of overflow
            throw outOfBoundException(offset, length);
        }
    }

    UnsupportedOperationException unsupportedAccessMode(int expected) {
        return new UnsupportedOperationException((String.format("Required access mode %s ; current access modes: %s",
                modeStrings(expected).get(0), modeStrings(mask))));
    }

    private List<String> modeStrings(int mode) {
        List<String> modes = new ArrayList<>();
        if ((mode & READ) != 0) {
            modes.add("READ");
        }
        if ((mode & WRITE) != 0) {
            modes.add("WRITE");
        }
        if ((mode & CLOSE) != 0) {
            modes.add("CLOSE");
        }
        if ((mode & SHARE) != 0) {
            modes.add("SHARE");
        }
        if ((mode & HANDOFF) != 0) {
            modes.add("HANDOFF");
        }
        return modes;
    }

    private IndexOutOfBoundsException outOfBoundException(long offset, long length) {
        return new IndexOutOfBoundsException(String.format("Out of bound access on segment %s; new offset = %d; new length = %d",
                this, offset, length));
    }

    protected int id() {
        //compute a stable and random id for this memory segment
        return Math.abs(Objects.hash(base(), min, NONCE));
    }

    static class SegmentSplitter implements Spliterator<MemorySegment> {
        MemorySegmentImpl segment;
        long elemCount;
        final long elementSize;
        long currentIndex;

        SegmentSplitter(long elementSize, long elemCount, MemorySegmentImpl segment) {
            this.segment = segment;
            this.elementSize = elementSize;
            this.elemCount = elemCount;
        }

        @Override
        public SegmentSplitter trySplit() {
            if (currentIndex == 0 && elemCount > 1) {
                MemorySegmentImpl parent = segment;
                long rem = elemCount % 2;
                long split = elemCount / 2;
                long lobound = split * elementSize;
                long hibound = lobound + (rem * elementSize);
                elemCount  = split + rem;
                segment = parent.asSliceNoCheck(lobound, hibound);
                return new SegmentSplitter(elementSize, split, parent.asSliceNoCheck(0, lobound));
            } else {
                return null;
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super MemorySegment> action) {
            Objects.requireNonNull(action);
            if (currentIndex < elemCount) {
                MemorySegmentImpl acquired = segment;
                try {
                    action.accept(acquired.asSliceNoCheck(currentIndex * elementSize, elementSize));
                } finally {
                    currentIndex++;
                    if (currentIndex == elemCount) {
                        segment = null;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super MemorySegment> action) {
            Objects.requireNonNull(action);
            if (currentIndex < elemCount) {
                MemorySegmentImpl acquired = segment;
                try {
                    if (acquired.isSmall()) {
                        int index = (int) currentIndex;
                        int limit = (int) elemCount;
                        int elemSize = (int) elementSize;
                        for (; index < limit; index++) {
                            action.accept(acquired.asSliceNoCheck(index * elemSize, elemSize));
                        }
                    } else {
                        for (long i = currentIndex ; i < elemCount ; i++) {
                            action.accept(acquired.asSliceNoCheck(i * elementSize, elementSize));
                        }
                    }
                } finally {
                    currentIndex = elemCount;
                    segment = null;
                }
            }
        }

        @Override
        public long estimateSize() {
            return elemCount;
        }

        @Override
        public int characteristics() {
            return NONNULL | SUBSIZED | SIZED | IMMUTABLE | ORDERED;
        }
    }

    // Object methods

    @Override
    public String toString() {
        return "MemorySegment{ id=0x" + Long.toHexString(id()) + " limit: " + length + " }";
    }

    public static MemorySegmentImpl ofBuffer(ByteBuffer bb) {
        long bbAddress = nioAccess.getBufferAddress(bb);
        Object base = nioAccess.getBufferBase(bb);
        UnmapperProxy unmapper = nioAccess.unmapper(bb);

        int pos = bb.position();
        int limit = bb.limit();
        int size = limit - pos;

        MemorySegmentImpl bufferSegment = (MemorySegmentImpl)nioAccess.bufferSegment(bb);
        final MemoryScope bufferScope;
        int modes;
        if (bufferSegment != null) {
            bufferScope = bufferSegment.scope;
            modes = bufferSegment.mask;
        } else {
            bufferScope = MemoryScope.createConfined(bb, MemoryScope.DUMMY_CLEANUP_ACTION, null);
            modes = defaultAccessModes(size);
        }
        if (bb.isReadOnly()) {
            modes &= ~WRITE;
        }
        return new MemorySegmentImpl(base != null ? BYTE_KIND : NO_KIND, bbAddress + pos, base,
                unmapper, size, modes, bufferScope);
    }

    // mapped segments support

    public static void load(MemorySegment segment) {
        MemorySegmentImpl segmentImpl = checkMappedSegment(segment);
        SCOPED_MEMORY_ACCESS.load(segmentImpl.scope, segmentImpl.min, segmentImpl.unmapper.isSync(), segmentImpl.length);
    }

    public static void unload(MemorySegment segment) {
        MemorySegmentImpl segmentImpl = checkMappedSegment(segment);
        SCOPED_MEMORY_ACCESS.unload(segmentImpl.scope, segmentImpl.min, segmentImpl.unmapper.isSync(), segmentImpl.length);
    }

    public static boolean isLoaded(MemorySegment segment) {
        MemorySegmentImpl segmentImpl = checkMappedSegment(segment);
        return SCOPED_MEMORY_ACCESS.isLoaded(segmentImpl.scope, segmentImpl.min, segmentImpl.unmapper.isSync(), segmentImpl.length);
    }

    public static void force(MemorySegment segment) {
        MemorySegmentImpl segmentImpl = checkMappedSegment(segment);
        SCOPED_MEMORY_ACCESS.force(segmentImpl.scope, segmentImpl.unmapper.fileDescriptor(), segmentImpl.min, segmentImpl.unmapper.isSync(), 0, segmentImpl.length);
    }

    private static MemorySegmentImpl checkMappedSegment(MemorySegment segment) {
        MemorySegmentImpl segmentImpl = (MemorySegmentImpl)segment;
        if (segmentImpl.unmapper == null) {
            throw new UnsupportedOperationException("Not a mapped memory segment");
        }
        return segmentImpl;
    }

    // factories

    public static final MemorySegment EVERYTHING = makeNativeSegmentUnchecked(MemoryAddress.NULL, Long.MAX_VALUE, MemoryScope.DUMMY_CLEANUP_ACTION, null)
            .share()
            .withAccessModes(READ | WRITE);

    private static final Unsafe unsafe = Unsafe.getUnsafe();

    private static final int BYTE_ARR_BASE = unsafe.arrayBaseOffset(byte[].class);

    // The maximum alignment supported by malloc - typically 16 on
    // 64-bit platforms and 8 on 32-bit platforms.
    private final static long MAX_MALLOC_ALIGN = Unsafe.ADDRESS_SIZE == 4 ? 8 : 16;

    private static final boolean skipZeroMemory = GetBooleanAction.privilegedGetProperty("jdk.internal.foreign.skipZeroMemory");

    public static MemorySegment makeNativeSegment(long bytesSize, long alignmentBytes) {
        if (VM.isDirectMemoryPageAligned()) {
            alignmentBytes = Math.max(alignmentBytes, nioAccess.pageSize());
        }
        long alignedSize = alignmentBytes > MAX_MALLOC_ALIGN ?
                bytesSize + (alignmentBytes - 1) :
                bytesSize;

        nioAccess.reserveMemory(alignedSize, bytesSize);

        long buf = unsafe.allocateMemory(alignedSize);
        if (!skipZeroMemory) {
            unsafe.setMemory(buf, alignedSize, (byte)0);
        }
        long alignedBuf = Utils.alignUp(buf, alignmentBytes);
        MemoryScope scope = MemoryScope.createConfined(null, () -> {
            unsafe.freeMemory(buf);
            nioAccess.unreserveMemory(alignedSize, bytesSize);
        }, null);
        MemorySegment segment = new MemorySegmentImpl(NO_KIND, buf, null, null, alignedSize,
                defaultAccessModes(alignedSize), scope);
        if (alignedSize != bytesSize) {
            long delta = alignedBuf - buf;
            segment = segment.asSlice(delta, bytesSize);
        }
        return segment;
    }

    public static MemorySegment makeNativeSegmentUnchecked(MemoryAddress min, long bytesSize, Runnable cleanupAction, Object ref) {
        return new MemorySegmentImpl(NO_KIND, min.toRawLongValue(), null, null, bytesSize, defaultAccessModes(bytesSize),
                MemoryScope.createConfined(ref, cleanupAction == null ? MemoryScope.DUMMY_CLEANUP_ACTION : cleanupAction, null));
    }

    public static MemorySegment makeArraySegment(byte[] arr) {
        return makeHeapSegment(BYTE_KIND, arr, arr.length,
                Unsafe.ARRAY_BYTE_BASE_OFFSET, Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static MemorySegment makeArraySegment(char[] arr) {
        return makeHeapSegment(CHAR_KIND, arr, arr.length,
                Unsafe.ARRAY_CHAR_BASE_OFFSET, Unsafe.ARRAY_CHAR_INDEX_SCALE);
    }

    public static MemorySegment makeArraySegment(short[] arr) {
        return makeHeapSegment(SHORT_KIND, arr, arr.length,
                Unsafe.ARRAY_SHORT_BASE_OFFSET, Unsafe.ARRAY_SHORT_INDEX_SCALE);
    }

    public static MemorySegment makeArraySegment(int[] arr) {
        return makeHeapSegment(INT_KIND, arr, arr.length,
                Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE);
    }

    public static MemorySegment makeArraySegment(long[] arr) {
        return makeHeapSegment(LONG_KIND, arr, arr.length,
                Unsafe.ARRAY_LONG_BASE_OFFSET, Unsafe.ARRAY_LONG_INDEX_SCALE);
    }

    public static MemorySegment makeArraySegment(float[] arr) {
        return makeHeapSegment(FLOAT_KIND, arr, arr.length,
                Unsafe.ARRAY_FLOAT_BASE_OFFSET, Unsafe.ARRAY_FLOAT_INDEX_SCALE);
    }

    public static MemorySegment makeArraySegment(double[] arr) {
        return makeHeapSegment(DOUBLE_KIND, arr, arr.length,
                Unsafe.ARRAY_DOUBLE_BASE_OFFSET, Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
    }

    static <Z> MemorySegment makeHeapSegment(byte kind, Z obj, int length, int base, int scale) {
        int byteSize = length * scale;
        MemoryScope scope = MemoryScope.createConfined(null, MemoryScope.DUMMY_CLEANUP_ACTION, null);
        return new MemorySegmentImpl(kind, base, obj, null, byteSize, defaultAccessModes(byteSize), scope);
    }

    public static MemorySegment makeMappedSegment(Path path, long bytesOffset, long bytesSize, FileChannel.MapMode mapMode) throws IOException {
        if (bytesSize < 0) throw new IllegalArgumentException("Requested bytes size must be >= 0.");
        if (bytesOffset < 0) throw new IllegalArgumentException("Requested bytes offset must be >= 0.");
        try (FileChannelImpl channelImpl = (FileChannelImpl)FileChannel.open(path, openOptions(mapMode))) {
            UnmapperProxy unmapperProxy = channelImpl.mapInternal(mapMode, bytesOffset, bytesSize);
            MemoryScope scope = MemoryScope.createConfined(null, unmapperProxy::unmap, null);
            int modes = MemorySegmentImpl.defaultAccessModes(bytesSize);
            if (mapMode == FileChannel.MapMode.READ_ONLY) {
                modes &= ~WRITE;
            }
            return new MemorySegmentImpl(NO_KIND, unmapperProxy.address(), null, unmapperProxy, bytesSize,
                    modes, scope);
        }
    }

    private static OpenOption[] openOptions(FileChannel.MapMode mapMode) {
        if (mapMode == FileChannel.MapMode.READ_ONLY) {
            return new OpenOption[] { StandardOpenOption.READ };
        } else if (mapMode == FileChannel.MapMode.READ_WRITE || mapMode == FileChannel.MapMode.PRIVATE) {
            return new OpenOption[] { StandardOpenOption.READ, StandardOpenOption.WRITE };
        } else {
            throw new UnsupportedOperationException("Unsupported map mode: " + mapMode);
        }
    }
}
