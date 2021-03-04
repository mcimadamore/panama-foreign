/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

/*
 * @test
 * @run testng TestLayoutPaths
 */

import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.SequenceLayout;

import org.testng.annotations.*;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;

import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;
import static org.testng.Assert.*;

public class TestLayoutPaths {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadBitSelectFromSeq() {
        SequenceLayout seq = MemoryLayout.ofSequence(JAVA_INT);
        seq.path().groupElement("foo").bitOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadByteSelectFromSeq() {
        SequenceLayout seq = MemoryLayout.ofSequence(JAVA_INT);
        seq.path().groupElement("foo").byteOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadBitSelectFromStruct() {
        GroupLayout g = MemoryLayout.ofStruct(JAVA_INT);
        g.path().sequenceElement().bitOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadByteSelectFromStruct() {
        GroupLayout g = MemoryLayout.ofStruct(JAVA_INT);
        g.path().sequenceElement().byteOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadBitSelectFromValue() {
        SequenceLayout seq = MemoryLayout.ofSequence(JAVA_INT);
        seq.path().sequenceElement().sequenceElement().bitOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadByteSelectFromValue() {
        SequenceLayout seq = MemoryLayout.ofSequence(JAVA_INT);
        seq.path().sequenceElement().sequenceElement().byteOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUnknownBitStructField() {
        GroupLayout g = MemoryLayout.ofStruct(JAVA_INT);
        g.path().groupElement("foo").bitOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUnknownByteStructField() {
        GroupLayout g = MemoryLayout.ofStruct(JAVA_INT);
        g.path().groupElement("foo").byteOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBitOutOfBoundsSeqIndex() {
        SequenceLayout seq = MemoryLayout.ofSequence(5, JAVA_INT);
        seq.path().sequenceElement(6).bitOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testByteOutOfBoundsSeqIndex() {
        SequenceLayout seq = MemoryLayout.ofSequence(5, JAVA_INT);
        seq.path().sequenceElement(6).byteOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNegativeSeqIndex() {
       JAVA_INT.path().sequenceElement(-2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBitNegativeSeqIndex() {
        SequenceLayout seq = MemoryLayout.ofSequence(5, JAVA_INT);
        seq.path().sequenceElement(-2).bitOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testByteNegativeSeqIndex() {
        SequenceLayout seq = MemoryLayout.ofSequence(5, JAVA_INT);
        seq.path().sequenceElement(-2).byteOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOutOfBoundsSeqRange() {
        SequenceLayout seq = MemoryLayout.ofSequence(5, JAVA_INT);
        seq.path().sequenceElement(6, 2).bitOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNegativeSeqRange() {
        JAVA_INT.path().sequenceElement(-2, 2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBitNegativeSeqRange() {
        SequenceLayout seq = MemoryLayout.ofSequence(5, JAVA_INT);
        seq.path().sequenceElement(-2, 2).bitOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testByteNegativeSeqRange() {
        SequenceLayout seq = MemoryLayout.ofSequence(5, JAVA_INT);
        seq.path().sequenceElement(-2, 2).byteOffset();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIncompleteAccess() {
        SequenceLayout seq = MemoryLayout.ofSequence(5, MemoryLayout.ofStruct(JAVA_INT));
        seq.path().sequenceElement().varHandle(int.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBitOffsetHandleBadRange() {
        SequenceLayout seq = MemoryLayout.ofSequence(5, MemoryLayout.ofStruct(JAVA_INT));
        seq.path().sequenceElement(0, 1).bitOffsetHandle(); // ranges not accepted
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testByteOffsetHandleBadRange() {
        SequenceLayout seq = MemoryLayout.ofSequence(5, MemoryLayout.ofStruct(JAVA_INT));
        seq.path().sequenceElement(0, 1).byteOffsetHandle(); // ranges not accepted
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testBadMultiple() {
        GroupLayout g = MemoryLayout.ofStruct(MemoryLayout.ofPaddingBits(3), JAVA_INT.withName("foo"));
        g.path().groupElement("foo").byteOffset();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testBitOffsetBadUnboundedSequenceTraverse() {
        MemoryLayout layout = MemoryLayout.ofSequence(MemoryLayout.ofSequence(JAVA_INT));
        layout.path().sequenceElement(1).sequenceElement(0).bitOffset();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testByteOffsetBadUnboundedSequenceTraverse() {
        MemoryLayout layout = MemoryLayout.ofSequence(MemoryLayout.ofSequence(JAVA_INT));
        layout.path().sequenceElement(1).sequenceElement(0).byteOffset();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testBitOffsetHandleBadUnboundedSequenceTraverse() {
        MemoryLayout layout = MemoryLayout.ofSequence(MemoryLayout.ofSequence(JAVA_INT));
        layout.path().sequenceElement(1).sequenceElement(0).bitOffsetHandle();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testByteOffsetHandleBadUnboundedSequenceTraverse() {
        MemoryLayout layout = MemoryLayout.ofSequence(MemoryLayout.ofSequence(JAVA_INT));
        layout.path().sequenceElement(1).sequenceElement(0).byteOffsetHandle();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testBadByteOffsetNoMultipleOf8() {
        MemoryLayout layout = MemoryLayout.ofStruct(MemoryLayout.ofPaddingBits(7), JAVA_INT.withName("x"));
        layout.path().groupElement("x").byteOffset();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testBadByteOffsetHandleNoMultipleOf8() throws Throwable {
        MemoryLayout layout = MemoryLayout.ofStruct(MemoryLayout.ofPaddingBits(7), JAVA_INT.withName("x"));
        MethodHandle handle = layout.path().groupElement("x").byteOffsetHandle();
        handle.invoke();
    }

    @Test
    public void testBadContainerAlign() {
        GroupLayout g = MemoryLayout.ofStruct(JAVA_INT.withBitAlignment(16).withName("foo")).withBitAlignment(8);
        try {
            g.path().groupElement("foo").bitOffset();
            g.path().groupElement("foo").byteOffset();
        } catch (Throwable ex) {
            throw new AssertionError(ex); // should be ok!
        }
        try {
            g.path().groupElement("foo").varHandle(int.class); //ok
            assertTrue(false); //should fail!
        } catch (UnsupportedOperationException ex) {
            //ok
        } catch (Throwable ex) {
            throw new AssertionError(ex); //should fail!
        }
    }

    @Test
    public void testBadAlignOffset() {
        GroupLayout g = MemoryLayout.ofStruct(MemoryLayouts.PAD_8, JAVA_INT.withBitAlignment(16).withName("foo"));
        try {
            g.path().groupElement("foo").bitOffset();
            g.path().groupElement("foo").byteOffset();
        } catch (Throwable ex) {
            throw new AssertionError(ex); // should be ok!
        }
        try {
            g.path().groupElement("foo").varHandle(int.class); //ok
            assertTrue(false); //should fail!
        } catch (UnsupportedOperationException ex) {
            //ok
        } catch (Throwable ex) {
            throw new AssertionError(ex); //should fail!
        }
    }

    @Test
    public void testBadSequencePathInOffset() {
        SequenceLayout seq = MemoryLayout.ofSequence(10, JAVA_INT);
        // bad path elements
        for (MemoryLayout.Path p : List.of( seq.path().sequenceElement(), seq.path().sequenceElement(0, 2) )) {
            try {
                p.bitOffset();
                fail();
            } catch (IllegalArgumentException ex) {
                assertTrue(true);
            }
            try {
                p.byteOffset();
                fail();
            } catch (IllegalArgumentException ex) {
                assertTrue(true);
            }
        }
    }

    @Test
    public void testBadSequencePathInSelect() {
        SequenceLayout seq = MemoryLayout.ofSequence(10, JAVA_INT);
        for (MemoryLayout.Path p : List.of( seq.path().sequenceElement(0), seq.path().sequenceElement(0, 2) )) {
            try {
                p.layout();
                fail();
            } catch (IllegalArgumentException ex) {
                assertTrue(true);
            }
        }
    }

    @Test
    public void testBadSequencePathInMap() {
        SequenceLayout seq = MemoryLayout.ofSequence(10, JAVA_INT);
        for (MemoryLayout.Path p : List.of( seq.path().sequenceElement(0), seq.path().sequenceElement(0, 2) )) {
            try {
                p.map(l -> l);
                fail();
            } catch (IllegalArgumentException ex) {
                assertTrue(true);
            }
        }
    }

    @Test
    public void testStructPaths() {
        long[] offsets = { 0, 8, 24, 56 };
        GroupLayout g = MemoryLayout.ofStruct(
                MemoryLayouts.JAVA_BYTE.withName("1"),
                MemoryLayouts.JAVA_CHAR.withName("2"),
                MemoryLayouts.JAVA_FLOAT.withName("3"),
                MemoryLayouts.JAVA_LONG.withName("4")
        );

        // test select

        for (int i = 1 ; i <= 4 ; i++) {
            MemoryLayout selected = g.path().groupElement(String.valueOf(i)).layout();
            assertTrue(selected == g.memberLayouts().get(i - 1));
        }

        // test offset

        for (int i = 1 ; i <= 4 ; i++) {
            long bitOffset = g.path().groupElement(String.valueOf(i)).bitOffset();
            assertEquals(offsets[i - 1], bitOffset);
            long byteOffset = g.path().groupElement(String.valueOf(i)).byteOffset();
            assertEquals((offsets[i - 1]) >>> 3, byteOffset);
        }

        // test map

        for (int i = 1 ; i <= 4 ; i++) {
            GroupLayout g2 = (GroupLayout)g.path().groupElement(String.valueOf(i)).map(l -> MemoryLayouts.JAVA_DOUBLE);
            assertTrue(g2.isStruct());
            for (int j = 0 ; j < 4 ; j++) {
                if (j == i - 1) {
                    assertEquals(g2.memberLayouts().get(j), MemoryLayouts.JAVA_DOUBLE);
                } else {
                    assertEquals(g2.memberLayouts().get(j), g.memberLayouts().get(j));
                }
            }
        }
    }

    @Test
    public void testUnionPaths() {
        long[] offsets = { 0, 0, 0, 0 };
        GroupLayout g = MemoryLayout.ofUnion(
                MemoryLayouts.JAVA_BYTE.withName("1"),
                MemoryLayouts.JAVA_CHAR.withName("2"),
                MemoryLayouts.JAVA_FLOAT.withName("3"),
                MemoryLayouts.JAVA_LONG.withName("4")
        );

        // test select

        for (int i = 1 ; i <= 4 ; i++) {
            MemoryLayout selected = g.path().groupElement(String.valueOf(i)).layout();
            assertTrue(selected == g.memberLayouts().get(i - 1));
        }

        // test offset

        for (int i = 1 ; i <= 4 ; i++) {
            long bitOffset = g.path().groupElement(String.valueOf(i)).bitOffset();
            assertEquals(offsets[i - 1], bitOffset);
            long byteOffset = g.path().groupElement(String.valueOf(i)).byteOffset();
            assertEquals((offsets[i - 1]) >>> 3, byteOffset);
        }

        // test map

        for (int i = 1 ; i <= 4 ; i++) {
            GroupLayout g2 = (GroupLayout)g.path().groupElement(String.valueOf(i)).map(l -> MemoryLayouts.JAVA_DOUBLE);
            assertTrue(g2.isUnion());
            for (int j = 0 ; j < 4 ; j++) {
                if (j == i - 1) {
                    assertEquals(g2.memberLayouts().get(j), MemoryLayouts.JAVA_DOUBLE);
                } else {
                    assertEquals(g2.memberLayouts().get(j), g.memberLayouts().get(j));
                }
            }
        }
    }

    @Test
    public void testSequencePaths() {
        long[] offsets = { 0, 8, 16, 24 };
        SequenceLayout g = MemoryLayout.ofSequence(4, MemoryLayouts.JAVA_BYTE);

        // test select

        MemoryLayout selected = g.path().sequenceElement().layout();
        assertTrue(selected == MemoryLayouts.JAVA_BYTE);

        // test offset

        for (int i = 0 ; i < 4 ; i++) {
            long bitOffset = g.path().sequenceElement(i).bitOffset();
            assertEquals(offsets[i], bitOffset);
            long byteOffset = g.path().sequenceElement(i).byteOffset();
            assertEquals((offsets[i]) >>> 3, byteOffset);
        }

        // test map

        SequenceLayout seq2 = (SequenceLayout)g.path().sequenceElement().map(l -> MemoryLayouts.JAVA_DOUBLE);
        assertTrue(seq2.elementLayout() == MemoryLayouts.JAVA_DOUBLE);
    }

    @Test(dataProvider =  "offsetHandleCases")
    public void testOffsetHandle(MemoryLayout.Path path, long[] indexes,
                                 long expectedBitOffset) throws Throwable {
        MethodHandle bitOffsetHandle = path.bitOffsetHandle();
        bitOffsetHandle = bitOffsetHandle.asSpreader(long[].class, indexes.length);
        long actualBitOffset = (long) bitOffsetHandle.invokeExact(indexes);
        assertEquals(actualBitOffset, expectedBitOffset);
        if (expectedBitOffset % 8 == 0) {
            MethodHandle byteOffsetHandle = path.byteOffsetHandle();
            byteOffsetHandle = byteOffsetHandle.asSpreader(long[].class, indexes.length);
            long actualByteOffset = (long) byteOffsetHandle.invokeExact(indexes);
            assertEquals(actualByteOffset, expectedBitOffset / 8);
        }
    }

    @DataProvider
    public static Object[][] offsetHandleCases() {
        List<Object[]> testCases = new ArrayList<>();

        testCases.add(new Object[] {
            MemoryLayout.ofSequence(10, JAVA_INT).path().sequenceElement(),
            new long[] { 4 },
            JAVA_INT.bitSize() * 4
        });
        MemoryLayout seq1 = MemoryLayout.ofSequence(10, MemoryLayout.ofStruct(JAVA_INT, JAVA_INT.withName("y")));
        testCases.add(new Object[] {
            seq1.path().sequenceElement().groupElement("y"),
            new long[] { 4 },
            (JAVA_INT.bitSize() * 2) * 4 + JAVA_INT.bitSize()
        });
        MemoryLayout seq2 = MemoryLayout.ofSequence(10, MemoryLayout.ofStruct(MemoryLayout.ofPaddingBits(5), JAVA_INT.withName("y")));
        testCases.add(new Object[] {
            seq2.path().sequenceElement().groupElement("y"),
            new long[] { 4 },
            (JAVA_INT.bitSize() + 5) * 4 + 5
        });
        testCases.add(new Object[] {
            MemoryLayout.ofSequence(10, JAVA_INT).path().sequenceElement(),
            new long[] { 4 },
            JAVA_INT.bitSize() * 4
        });

        MemoryLayout struct = MemoryLayout.ofStruct(
                MemoryLayout.ofSequence(10, JAVA_INT).withName("data")
        );

        testCases.add(new Object[] {
            struct.path().groupElement("data").sequenceElement(),
            new long[] { 4 },
            JAVA_INT.bitSize() * 4
        });

        MemoryLayout complexLayout = MemoryLayout.ofStruct(
            MemoryLayout.ofSequence(10,
                MemoryLayout.ofSequence(10,
                    MemoryLayout.ofStruct(
                        JAVA_INT.withName("x"),
                        JAVA_INT.withName("y")
                    )
                )
            ).withName("data")
        );

        testCases.add(new Object[] {
            complexLayout.path().groupElement("data").sequenceElement().sequenceElement().groupElement("x"),
            new long[] { 0, 1 },
            (JAVA_INT.bitSize() * 2)
        });
        testCases.add(new Object[] {
            complexLayout.path().groupElement("data").sequenceElement().sequenceElement().groupElement("x"),
            new long[] { 1, 0 },
            (JAVA_INT.bitSize() * 2) * 10
        });
        testCases.add(new Object[] {
            complexLayout.path().groupElement("data").sequenceElement().sequenceElement().groupElement("y"),
            new long[] { 0, 1 },
            (JAVA_INT.bitSize() * 2) + JAVA_INT.bitSize()
        });
        testCases.add(new Object[] {
            complexLayout.path().groupElement("data").sequenceElement().sequenceElement().groupElement("y"),
            new long[] { 1, 0 },
            (JAVA_INT.bitSize() * 2) * 10 + JAVA_INT.bitSize()
        });

        return testCases.toArray(Object[][]::new);
    }

}

