/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * or visit www.oracle.com if you need additional information or have
 * questions.
 */
package jdk.incubator.vector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import static jdk.incubator.vector.VectorIntrinsics.*;

@SuppressWarnings("cast")
final class Byte64Vector extends ByteVector {
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_64;

    static final Byte64Vector ZERO = new Byte64Vector();

    static final int LENGTH = SPECIES.length();

    private final byte[] vec; // Don't access directly, use getElements() instead.

    private byte[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    Byte64Vector() {
        vec = new byte[SPECIES.length()];
    }

    Byte64Vector(byte[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    Byte64Vector uOp(FUnOp f) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new Byte64Vector(res);
    }

    @Override
    Byte64Vector uOp(VectorMask<Byte> o, FUnOp f) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        boolean[] mbits = ((Byte64Mask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new Byte64Vector(res);
    }

    // Binary operator

    @Override
    Byte64Vector bOp(Vector<Byte> o, FBinOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.getElements();
        byte[] vec2 = ((Byte64Vector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Byte64Vector(res);
    }

    @Override
    Byte64Vector bOp(Vector<Byte> o1, VectorMask<Byte> o2, FBinOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.getElements();
        byte[] vec2 = ((Byte64Vector)o1).getElements();
        boolean[] mbits = ((Byte64Mask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new Byte64Vector(res);
    }

    // Trinary operator

    @Override
    Byte64Vector tOp(Vector<Byte> o1, Vector<Byte> o2, FTriOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = this.getElements();
        byte[] vec2 = ((Byte64Vector)o1).getElements();
        byte[] vec3 = ((Byte64Vector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new Byte64Vector(res);
    }

    @Override
    Byte64Vector tOp(Vector<Byte> o1, Vector<Byte> o2, VectorMask<Byte> o3, FTriOp f) {
        byte[] res = new byte[length()];
        byte[] vec1 = getElements();
        byte[] vec2 = ((Byte64Vector)o1).getElements();
        byte[] vec3 = ((Byte64Vector)o2).getElements();
        boolean[] mbits = ((Byte64Mask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new Byte64Vector(res);
    }

    @Override
    byte rOp(byte v, FBinOp f) {
        byte[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    @Override
    @ForceInline
    public <F> Vector<F> cast(VectorSpecies<F> s) {
        Objects.requireNonNull(s);
        if (s.length() != LENGTH)
            throw new IllegalArgumentException("Vector length this species length differ");

        return VectorIntrinsics.cast(
            Byte64Vector.class,
            byte.class, LENGTH,
            s.vectorType(),
            s.elementType(), LENGTH,
            this, s,
            (species, vector) -> vector.castDefault(species)
        );
    }

    @SuppressWarnings("unchecked")
    @ForceInline
    private <F> Vector<F> castDefault(VectorSpecies<F> s) {
        int limit = s.length();

        Class<?> stype = s.elementType();
        if (stype == byte.class) {
            byte[] a = new byte[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (byte) this.lane(i);
            }
            return (Vector) ByteVector.fromArray((VectorSpecies<Byte>) s, a, 0);
        } else if (stype == short.class) {
            short[] a = new short[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (short) this.lane(i);
            }
            return (Vector) ShortVector.fromArray((VectorSpecies<Short>) s, a, 0);
        } else if (stype == int.class) {
            int[] a = new int[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (int) this.lane(i);
            }
            return (Vector) IntVector.fromArray((VectorSpecies<Integer>) s, a, 0);
        } else if (stype == long.class) {
            long[] a = new long[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (long) this.lane(i);
            }
            return (Vector) LongVector.fromArray((VectorSpecies<Long>) s, a, 0);
        } else if (stype == float.class) {
            float[] a = new float[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (float) this.lane(i);
            }
            return (Vector) FloatVector.fromArray((VectorSpecies<Float>) s, a, 0);
        } else if (stype == double.class) {
            double[] a = new double[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (double) this.lane(i);
            }
            return (Vector) DoubleVector.fromArray((VectorSpecies<Double>) s, a, 0);
        } else {
            throw new UnsupportedOperationException("Bad lane type for casting.");
        }
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    public <F> Vector<F> reinterpret(VectorSpecies<F> s) {
        Objects.requireNonNull(s);

        if(s.elementType().equals(byte.class)) {
            return (Vector<F>) reshape((VectorSpecies<Byte>)s);
        }
        if(s.bitSize() == bitSize()) {
            return reinterpretType(s);
        }

        return defaultReinterpret(s);
    }

    @ForceInline
    private <F> Vector<F> reinterpretType(VectorSpecies<F> s) {
        Objects.requireNonNull(s);

        Class<?> stype = s.elementType();
        if (stype == byte.class) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                Byte64Vector.class,
                byte.class, Byte64Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == short.class) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                Short64Vector.class,
                short.class, Short64Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == int.class) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                Int64Vector.class,
                int.class, Int64Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == long.class) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                Long64Vector.class,
                long.class, Long64Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == float.class) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                Float64Vector.class,
                float.class, Float64Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == double.class) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                Double64Vector.class,
                double.class, Double64Vector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else {
            throw new UnsupportedOperationException("Bad lane type for casting.");
        }
    }

    @Override
    @ForceInline
    public ByteVector reshape(VectorSpecies<Byte> s) {
        Objects.requireNonNull(s);
        if (s.bitSize() == 64 && (s.vectorType() == Byte64Vector.class)) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                Byte64Vector.class,
                byte.class, Byte64Vector.LENGTH,
                this, s,
                (species, vector) -> (ByteVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 128 && (s.vectorType() == Byte128Vector.class)) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                Byte128Vector.class,
                byte.class, Byte128Vector.LENGTH,
                this, s,
                (species, vector) -> (ByteVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 256 && (s.vectorType() == Byte256Vector.class)) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                Byte256Vector.class,
                byte.class, Byte256Vector.LENGTH,
                this, s,
                (species, vector) -> (ByteVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 512 && (s.vectorType() == Byte512Vector.class)) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                Byte512Vector.class,
                byte.class, Byte512Vector.LENGTH,
                this, s,
                (species, vector) -> (ByteVector) vector.defaultReinterpret(species)
            );
        } else if ((s.bitSize() > 0) && (s.bitSize() <= 2048)
                && (s.bitSize() % 128 == 0) && (s.vectorType() == ByteMaxVector.class)) {
            return VectorIntrinsics.reinterpret(
                Byte64Vector.class,
                byte.class, LENGTH,
                ByteMaxVector.class,
                byte.class, ByteMaxVector.LENGTH,
                this, s,
                (species, vector) -> (ByteVector) vector.defaultReinterpret(species)
            );
        } else {
            throw new InternalError("Unimplemented size");
        }
    }

    // Binary operations with scalars

    @Override
    @ForceInline
    public ByteVector add(byte o) {
        return add((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public ByteVector add(byte o, VectorMask<Byte> m) {
        return add((Byte64Vector)ByteVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public ByteVector sub(byte o) {
        return sub((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public ByteVector sub(byte o, VectorMask<Byte> m) {
        return sub((Byte64Vector)ByteVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public ByteVector mul(byte o) {
        return mul((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public ByteVector mul(byte o, VectorMask<Byte> m) {
        return mul((Byte64Vector)ByteVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public ByteVector min(byte o) {
        return min((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public ByteVector max(byte o) {
        return max((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Byte> equal(byte o) {
        return equal((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Byte> notEqual(byte o) {
        return notEqual((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Byte> lessThan(byte o) {
        return lessThan((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Byte> lessThanEq(byte o) {
        return lessThanEq((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Byte> greaterThan(byte o) {
        return greaterThan((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public VectorMask<Byte> greaterThanEq(byte o) {
        return greaterThanEq((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public ByteVector blend(byte o, VectorMask<Byte> m) {
        return blend((Byte64Vector)ByteVector.broadcast(SPECIES, o), m);
    }


    @Override
    @ForceInline
    public ByteVector and(byte o) {
        return and((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public ByteVector and(byte o, VectorMask<Byte> m) {
        return and((Byte64Vector)ByteVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public ByteVector or(byte o) {
        return or((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public ByteVector or(byte o, VectorMask<Byte> m) {
        return or((Byte64Vector)ByteVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public ByteVector xor(byte o) {
        return xor((Byte64Vector)ByteVector.broadcast(SPECIES, o));
    }

    @Override
    @ForceInline
    public ByteVector xor(byte o, VectorMask<Byte> m) {
        return xor((Byte64Vector)ByteVector.broadcast(SPECIES, o), m);
    }

    @Override
    @ForceInline
    public Byte64Vector neg() {
        return (Byte64Vector)zero(SPECIES).sub(this);
    }

    // Unary operations

    @ForceInline
    @Override
    public Byte64Vector neg(VectorMask<Byte> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public Byte64Vector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, Byte64Vector.class, byte.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (byte) Math.abs(a)));
    }

    @ForceInline
    @Override
    public Byte64Vector abs(VectorMask<Byte> m) {
        return blend(abs(), m);
    }


    @Override
    @ForceInline
    public Byte64Vector not() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NOT, Byte64Vector.class, byte.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (byte) ~a));
    }

    @ForceInline
    @Override
    public Byte64Vector not(VectorMask<Byte> m) {
        return blend(not(), m);
    }
    // Binary operations

    @Override
    @ForceInline
    public Byte64Vector add(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, Byte64Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a + b)));
    }

    @Override
    @ForceInline
    public Byte64Vector add(Vector<Byte> v, VectorMask<Byte> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public Byte64Vector sub(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, Byte64Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a - b)));
    }

    @Override
    @ForceInline
    public Byte64Vector sub(Vector<Byte> v, VectorMask<Byte> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public Byte64Vector mul(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, Byte64Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a * b)));
    }

    @Override
    @ForceInline
    public Byte64Vector mul(Vector<Byte> v, VectorMask<Byte> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public Byte64Vector min(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;
        return (Byte64Vector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, Byte64Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte) Math.min(a, b)));
    }

    @Override
    @ForceInline
    public Byte64Vector min(Vector<Byte> v, VectorMask<Byte> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public Byte64Vector max(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, Byte64Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte) Math.max(a, b)));
        }

    @Override
    @ForceInline
    public Byte64Vector max(Vector<Byte> v, VectorMask<Byte> m) {
        return blend(max(v), m);
    }

    @Override
    @ForceInline
    public Byte64Vector and(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_AND, Byte64Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a & b)));
    }

    @Override
    @ForceInline
    public Byte64Vector or(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_OR, Byte64Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a | b)));
    }

    @Override
    @ForceInline
    public Byte64Vector xor(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_XOR, Byte64Vector.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (byte)(a ^ b)));
    }

    @Override
    @ForceInline
    public Byte64Vector and(Vector<Byte> v, VectorMask<Byte> m) {
        return blend(and(v), m);
    }

    @Override
    @ForceInline
    public Byte64Vector or(Vector<Byte> v, VectorMask<Byte> m) {
        return blend(or(v), m);
    }

    @Override
    @ForceInline
    public Byte64Vector xor(Vector<Byte> v, VectorMask<Byte> m) {
        return blend(xor(v), m);
    }

    @Override
    @ForceInline
    public Byte64Vector shiftLeft(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_LSHIFT, Byte64Vector.class, byte.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (byte) (a << (i & 0x7))));
    }

    @Override
    @ForceInline
    public Byte64Vector shiftLeft(int s, VectorMask<Byte> m) {
        return blend(shiftLeft(s), m);
    }

    @Override
    @ForceInline
    public Byte64Vector shiftLeft(Vector<Byte> s) {
        Byte64Vector shiftv = (Byte64Vector)s;
        // As per shift specification for Java, mask the shift count.
        shiftv = shiftv.and(ByteVector.broadcast(SPECIES, (byte) 0x7));
        return this.bOp(shiftv, (i, a, b) -> (byte) (a << (b & 0x7)));
    }

    @Override
    @ForceInline
    public Byte64Vector shiftRight(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_URSHIFT, Byte64Vector.class, byte.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (byte) ((a & 0xFF) >>> (i & 0x7))));
    }

    @Override
    @ForceInline
    public Byte64Vector shiftRight(int s, VectorMask<Byte> m) {
        return blend(shiftRight(s), m);
    }

    @Override
    @ForceInline
    public Byte64Vector shiftRight(Vector<Byte> s) {
        Byte64Vector shiftv = (Byte64Vector)s;
        // As per shift specification for Java, mask the shift count.
        shiftv = shiftv.and(ByteVector.broadcast(SPECIES, (byte) 0x7));
        return this.bOp(shiftv, (i, a, b) -> (byte) (a >>> (b & 0x7)));
    }

    @Override
    @ForceInline
    public Byte64Vector shiftArithmeticRight(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_RSHIFT, Byte64Vector.class, byte.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (byte) (a >> (i & 0x7))));
    }

    @Override
    @ForceInline
    public Byte64Vector shiftArithmeticRight(int s, VectorMask<Byte> m) {
        return blend(shiftArithmeticRight(s), m);
    }

    @Override
    @ForceInline
    public Byte64Vector shiftArithmeticRight(Vector<Byte> s) {
        Byte64Vector shiftv = (Byte64Vector)s;
        // As per shift specification for Java, mask the shift count.
        shiftv = shiftv.and(ByteVector.broadcast(SPECIES, (byte) 0x7));
        return this.bOp(shiftv, (i, a, b) -> (byte) (a >> (b & 0x7)));
    }

    // Ternary operations


    // Type specific horizontal reductions

    @Override
    @ForceInline
    public byte addLanes() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_ADD, Byte64Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 0, (i, a, b) -> (byte) (a + b)));
    }

    @Override
    @ForceInline
    public byte andLanes() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_AND, Byte64Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) -1, (i, a, b) -> (byte) (a & b)));
    }

    @Override
    @ForceInline
    public byte andLanes(VectorMask<Byte> m) {
        return ByteVector.broadcast(SPECIES, (byte) -1).blend(this, m).andLanes();
    }

    @Override
    @ForceInline
    public byte minLanes() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MIN, Byte64Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp(Byte.MAX_VALUE , (i, a, b) -> (byte) Math.min(a, b)));
    }

    @Override
    @ForceInline
    public byte maxLanes() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MAX, Byte64Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp(Byte.MIN_VALUE , (i, a, b) -> (byte) Math.max(a, b)));
    }

    @Override
    @ForceInline
    public byte mulLanes() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MUL, Byte64Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 1, (i, a, b) -> (byte) (a * b)));
    }

    @Override
    @ForceInline
    public byte orLanes() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_OR, Byte64Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 0, (i, a, b) -> (byte) (a | b)));
    }

    @Override
    @ForceInline
    public byte orLanes(VectorMask<Byte> m) {
        return ByteVector.broadcast(SPECIES, (byte) 0).blend(this, m).orLanes();
    }

    @Override
    @ForceInline
    public byte xorLanes() {
        return (byte) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_XOR, Byte64Vector.class, byte.class, LENGTH,
            this,
            v -> (long) v.rOp((byte) 0, (i, a, b) -> (byte) (a ^ b)));
    }

    @Override
    @ForceInline
    public byte xorLanes(VectorMask<Byte> m) {
        return ByteVector.broadcast(SPECIES, (byte) 0).blend(this, m).xorLanes();
    }


    @Override
    @ForceInline
    public byte addLanes(VectorMask<Byte> m) {
        return ByteVector.broadcast(SPECIES, (byte) 0).blend(this, m).addLanes();
    }


    @Override
    @ForceInline
    public byte mulLanes(VectorMask<Byte> m) {
        return ByteVector.broadcast(SPECIES, (byte) 1).blend(this, m).mulLanes();
    }

    @Override
    @ForceInline
    public byte minLanes(VectorMask<Byte> m) {
        return ByteVector.broadcast(SPECIES, Byte.MAX_VALUE).blend(this, m).minLanes();
    }

    @Override
    @ForceInline
    public byte maxLanes(VectorMask<Byte> m) {
        return ByteVector.broadcast(SPECIES, Byte.MIN_VALUE).blend(this, m).maxLanes();
    }

    @Override
    @ForceInline
    public VectorShuffle<Byte> toShuffle() {
        byte[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return VectorShuffle.fromArray(SPECIES, sa, 0);
    }

    // Memory operations

    private static final int ARRAY_SHIFT         = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_BYTE_INDEX_SCALE);
    private static final int BOOLEAN_ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_BOOLEAN_INDEX_SCALE);

    @Override
    @ForceInline
    public void intoArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
        VectorIntrinsics.store(Byte64Vector.class, byte.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(byte[] a, int ax, VectorMask<Byte> m) {
        ByteVector oldVal = ByteVector.fromArray(SPECIES, a, ax);
        ByteVector newVal = oldVal.blend(this, m);
        newVal.intoArray(a, ax);
    }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(Byte64Vector.class, byte.class, LENGTH,
                               a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = ByteBuffer.wrap(c, idx, c.length - idx).order(ByteOrder.nativeOrder());
                                   ByteBuffer tb = bbc;
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public final void intoByteArray(byte[] a, int ix, VectorMask<Byte> m) {
        Byte64Vector oldVal = (Byte64Vector) ByteVector.fromByteArray(SPECIES, a, ix);
        Byte64Vector newVal = oldVal.blend(this, m);
        newVal.intoByteArray(a, ix);
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix) {
        if (bb.order() != ByteOrder.nativeOrder()) {
            throw new IllegalArgumentException();
        }
        if (bb.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
        VectorIntrinsics.store(Byte64Vector.class, byte.class, LENGTH,
                               U.getReference(bb, BYTE_BUFFER_HB), ix + U.getLong(bb, BUFFER_ADDRESS),
                               this,
                               bb, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                   ByteBuffer tb = bbc;
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix, VectorMask<Byte> m) {
        Byte64Vector oldVal = (Byte64Vector) ByteVector.fromByteBuffer(SPECIES, bb, ix);
        Byte64Vector newVal = oldVal.blend(this, m);
        newVal.intoByteBuffer(bb, ix);
    }

    //

    @Override
    public String toString() {
        return Arrays.toString(getElements());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        Byte64Vector that = (Byte64Vector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    Byte64Mask bTest(Vector<Byte> o, FBinTest f) {
        byte[] vec1 = getElements();
        byte[] vec2 = ((Byte64Vector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new Byte64Mask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public Byte64Mask equal(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;

        return VectorIntrinsics.compare(
            BT_eq, Byte64Vector.class, Byte64Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public Byte64Mask notEqual(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;

        return VectorIntrinsics.compare(
            BT_ne, Byte64Vector.class, Byte64Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public Byte64Mask lessThan(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;

        return VectorIntrinsics.compare(
            BT_lt, Byte64Vector.class, Byte64Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public Byte64Mask lessThanEq(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;

        return VectorIntrinsics.compare(
            BT_le, Byte64Vector.class, Byte64Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public Byte64Mask greaterThan(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;

        return (Byte64Mask) VectorIntrinsics.compare(
            BT_gt, Byte64Vector.class, Byte64Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public Byte64Mask greaterThanEq(Vector<Byte> o) {
        Objects.requireNonNull(o);
        Byte64Vector v = (Byte64Vector)o;

        return VectorIntrinsics.compare(
            BT_ge, Byte64Vector.class, Byte64Mask.class, byte.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a >= b));
    }

    // Foreach

    @Override
    void forEach(FUnCon f) {
        byte[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            f.apply(i, vec[i]);
        }
    }

    @Override
    void forEach(VectorMask<Byte> o, FUnCon f) {
        boolean[] mbits = ((Byte64Mask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }



    @Override
    public Byte64Vector rotateLanesLeft(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new Byte64Vector(res);
    }

    @Override
    public Byte64Vector rotateLanesRight(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length(); i++){
            int z = i - j;
            if(j < 0) {
                res[length() + z] = vec[i];
            } else {
                res[z] = vec[i];
            }
        }
        return new Byte64Vector(res);
    }

    @Override
    public Byte64Vector shiftLanesLeft(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new Byte64Vector(res);
    }

    @Override
    public Byte64Vector shiftLanesRight(int j) {
        byte[] vec = getElements();
        byte[] res = new byte[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new Byte64Vector(res);
    }

    @Override
    @ForceInline
    public Byte64Vector rearrange(Vector<Byte> v,
                                  VectorShuffle<Byte> s, VectorMask<Byte> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public Byte64Vector rearrange(VectorShuffle<Byte> o1) {
        Objects.requireNonNull(o1);
        Byte64Shuffle s =  (Byte64Shuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            Byte64Vector.class, Byte64Shuffle.class, byte.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
                int ei = s_.lane(i);
                return v1.lane(ei);
            }));
    }

    @Override
    @ForceInline
    public Byte64Vector blend(Vector<Byte> o1, VectorMask<Byte> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        Byte64Vector v = (Byte64Vector)o1;
        Byte64Mask   m = (Byte64Mask)o2;

        return VectorIntrinsics.blend(
            Byte64Vector.class, Byte64Mask.class, byte.class, LENGTH,
            this, v, m,
            (v1, v2, m_) -> v1.bOp(v2, (i, a, b) -> m_.lane(i) ? b : a));
    }

    // Accessors

    @Override
    public byte lane(int i) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return (byte) VectorIntrinsics.extract(
                                Byte64Vector.class, byte.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    byte[] vecarr = vec.getElements();
                                    return (long)vecarr[ix];
                                });
    }

    @Override
    public Byte64Vector with(int i, byte e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                Byte64Vector.class, byte.class, LENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    byte[] res = v.getElements().clone();
                                    res[ix] = (byte)bits;
                                    return new Byte64Vector(res);
                                });
    }

    // Mask

    static final class Byte64Mask extends AbstractMask<Byte> {
        static final Byte64Mask TRUE_MASK = new Byte64Mask(true);
        static final Byte64Mask FALSE_MASK = new Byte64Mask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public Byte64Mask(boolean[] bits) {
            this(bits, 0);
        }

        public Byte64Mask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public Byte64Mask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        Byte64Mask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new Byte64Mask(res);
        }

        @Override
        Byte64Mask bOp(VectorMask<Byte> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((Byte64Mask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new Byte64Mask(res);
        }

        @Override
        public VectorSpecies<Byte> species() {
            return SPECIES;
        }

        @Override
        public Byte64Vector toVector() {
            byte[] res = new byte[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (byte) (bits[i] ? -1 : 0);
            }
            return new Byte64Vector(res);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <E> VectorMask<E> cast(VectorSpecies<E> species) {
            if (length() != species.length())
                throw new IllegalArgumentException("VectorMask length and species length differ");
            Class<?> stype = species.elementType();
            boolean [] maskArray = toArray();
            if (stype == byte.class) {
                return (VectorMask <E>) new Byte64Vector.Byte64Mask(maskArray);
            } else if (stype == short.class) {
                return (VectorMask <E>) new Short64Vector.Short64Mask(maskArray);
            } else if (stype == int.class) {
                return (VectorMask <E>) new Int64Vector.Int64Mask(maskArray);
            } else if (stype == long.class) {
                return (VectorMask <E>) new Long64Vector.Long64Mask(maskArray);
            } else if (stype == float.class) {
                return (VectorMask <E>) new Float64Vector.Float64Mask(maskArray);
            } else if (stype == double.class) {
                return (VectorMask <E>) new Double64Vector.Double64Mask(maskArray);
            } else {
                throw new UnsupportedOperationException("Bad lane type for casting.");
            }
        }

        // Unary operations

        @Override
        @ForceInline
        public Byte64Mask not() {
            return (Byte64Mask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, Byte64Mask.class, byte.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public Byte64Mask and(VectorMask<Byte> o) {
            Objects.requireNonNull(o);
            Byte64Mask m = (Byte64Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, Byte64Mask.class, byte.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public Byte64Mask or(VectorMask<Byte> o) {
            Objects.requireNonNull(o);
            Byte64Mask m = (Byte64Mask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, Byte64Mask.class, byte.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(BT_ne, Byte64Mask.class, byte.class, LENGTH,
                                         this, this,
                                         (m, __) -> anyTrueHelper(((Byte64Mask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(BT_overflow, Byte64Mask.class, byte.class, LENGTH,
                                         this, VectorMask.maskAllTrue(species()),
                                         (m, __) -> allTrueHelper(((Byte64Mask)m).getBits()));
        }
    }

    // Shuffle

    static final class Byte64Shuffle extends AbstractShuffle<Byte> {
        Byte64Shuffle(byte[] reorder) {
            super(reorder);
        }

        public Byte64Shuffle(int[] reorder) {
            super(reorder);
        }

        public Byte64Shuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public Byte64Shuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public VectorSpecies<Byte> species() {
            return SPECIES;
        }

        @Override
        public ByteVector toVector() {
            byte[] va = new byte[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (byte) lane(i);
            }
            return ByteVector.fromArray(SPECIES, va, 0);
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        public <F> VectorShuffle<F> cast(VectorSpecies<F> species) {
            if (length() != species.length())
                throw new IllegalArgumentException("Shuffle length and species length differ");
            Class<?> stype = species.elementType();
            int [] shuffleArray = toArray();
            if (stype == byte.class) {
                return (VectorShuffle<F>) new Byte64Vector.Byte64Shuffle(shuffleArray);
            } else if (stype == short.class) {
                return (VectorShuffle<F>) new Short64Vector.Short64Shuffle(shuffleArray);
            } else if (stype == int.class) {
                return (VectorShuffle<F>) new Int64Vector.Int64Shuffle(shuffleArray);
            } else if (stype == long.class) {
                return (VectorShuffle<F>) new Long64Vector.Long64Shuffle(shuffleArray);
            } else if (stype == float.class) {
                return (VectorShuffle<F>) new Float64Vector.Float64Shuffle(shuffleArray);
            } else if (stype == double.class) {
                return (VectorShuffle<F>) new Double64Vector.Double64Shuffle(shuffleArray);
            } else {
                throw new UnsupportedOperationException("Bad lane type for casting.");
            }
        }

        @Override
        public Byte64Shuffle rearrange(VectorShuffle<Byte> o) {
            Byte64Shuffle s = (Byte64Shuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new Byte64Shuffle(r);
        }
    }

    // VectorSpecies

    @Override
    public VectorSpecies<Byte> species() {
        return SPECIES;
    }
}