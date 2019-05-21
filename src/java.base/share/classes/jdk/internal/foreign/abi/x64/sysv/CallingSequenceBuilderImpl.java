/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package jdk.internal.foreign.abi.x64.sysv;

import java.foreign.layout.Address;
import java.foreign.layout.Group;
import java.foreign.layout.Group.Kind;
import java.foreign.layout.Layout;
import java.foreign.layout.Padding;
import java.foreign.layout.Sequence;
import java.foreign.layout.Value;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import jdk.internal.foreign.Util;
import jdk.internal.foreign.abi.Argument;
import jdk.internal.foreign.abi.ArgumentBinding;
import jdk.internal.foreign.abi.CallingSequenceBuilder;
import jdk.internal.foreign.abi.Storage;
import jdk.internal.foreign.abi.StorageClass;
import jdk.internal.foreign.abi.x64.ArgumentClass;
import jdk.internal.foreign.abi.x64.SharedUtils;

import static sun.security.action.GetBooleanAction.privilegedGetProperty;

public class CallingSequenceBuilderImpl extends CallingSequenceBuilder {

    private static final SharedUtils.StorageDebugHelper storageDbgHelper = new SharedUtils.StorageDebugHelper(
            new String[] { "rdi", "rsi", "rdx", "rcx", "r8", "r9" },
            new String[] { "rax", "rdx" },
            new String[] { "st0", "st1" },
            SysVx64ABI.MAX_VECTOR_ARGUMENT_REGISTERS,
            SysVx64ABI.MAX_VECTOR_RETURN_REGISTERS
    );

    private static final boolean DEBUG =
        privilegedGetProperty("jdk.internal.foreign.abi.x64.sysv.DEBUG");

    // The AVX 512 enlightened ABI says "eight eightbytes"
    // Although AMD64 0.99.6 states 4 eightbytes
    private static final int MAX_AGGREGATE_REGS_SIZE = 8;

    private static final ArrayList<ArgumentClass> COMPLEX_X87_CLASSES;

    static {
        COMPLEX_X87_CLASSES = new ArrayList<>();
        COMPLEX_X87_CLASSES.add(ArgumentClass.X87);
        COMPLEX_X87_CLASSES.add(ArgumentClass.X87UP);
        COMPLEX_X87_CLASSES.add(ArgumentClass.X87);
        COMPLEX_X87_CLASSES.add(ArgumentClass.X87UP);
    }

    public CallingSequenceBuilderImpl(Layout layout) {
        this(layout, new StorageCalculator(false), new StorageCalculator(true));
    }

    private CallingSequenceBuilderImpl(Layout layout, StorageCalculator retCalculator, StorageCalculator argCalculator) {
        super(layout, retCalculator::addBindings, argCalculator::addBindings, argCalculator::addBindings);
    }

    @Override
    protected ArgumentInfo makeArgument(Layout layout, int pos, String name) {
        return new ArgumentInfo(layout, pos, name);
    }

    static class ArgumentInfo extends Argument {
        private final List<ArgumentClass> classes;

        public ArgumentInfo(Layout layout, int argumentIndex, String debugName) {
            super(layout, argumentIndex, debugName);
            this.classes = classifyType(layout);
        }

        public int getIntegerRegs() {
            return (int)classes.stream()
                    .filter(cl -> cl == ArgumentClass.INTEGER)
                    .count();
        }

        public int getVectorRegs() {
            return (int)classes.stream()
                    .filter(cl -> cl == ArgumentClass.SSE)
                    .count();
        }

        @Override
        public boolean inMemory() {
            return classes.stream().allMatch(this::isMemoryClass);
        }

        private boolean isMemoryClass(ArgumentClass cl) {
            return cl == ArgumentClass.MEMORY ||
                    (argumentIndex() != -1 &&
                            (cl == ArgumentClass.X87 || cl == ArgumentClass.X87UP));
        }

        public List<ArgumentClass> getClasses() {
            return classes;
        }
    }

    private static List<ArgumentClass> classifyValueType(Value type) {
        ArrayList<ArgumentClass> classes = new ArrayList<>();

        switch (type.kind()) {
            case INTEGRAL_SIGNED: case INTEGRAL_UNSIGNED:
                classes.add(ArgumentClass.INTEGER);
                // int128
                long left = (type.bitsSize() / 8) - 8;
                while (left > 0) {
                    classes.add(ArgumentClass.INTEGER);
                    left -= 8;
                }
                return classes;
            case FLOATING_POINT:
                if ((type.bitsSize() / 8) > 8) {
                    classes.add(ArgumentClass.X87);
                    classes.add(ArgumentClass.X87UP);
                    return classes;
                } else {
                    classes.add(ArgumentClass.SSE);
                    return classes;
                }
            default:
                throw new IllegalArgumentException("Type " + type + " is not yet supported");
        }
    }

    private static List<ArgumentClass> classifyArrayType(Sequence type) {
        long nWords = Util.alignUp((type.bitsSize() / 8), 8) / 8;
        if (nWords > MAX_AGGREGATE_REGS_SIZE) {
            return createMemoryClassArray(nWords);
        }

        ArrayList<ArgumentClass> classes = new ArrayList<>();

        for (long i = 0; i < nWords; i++) {
            classes.add(ArgumentClass.NO_CLASS);
        }

        long offset = 0;
        final long count = type.elementsSize();
        for (long idx = 0; idx < count; idx++) {
            Layout t = type.element();
            offset = SharedUtils.align(t, false, offset);
            List<ArgumentClass> subclasses = classifyType(t);
            if (subclasses.isEmpty()) {
                return classes;
            }

            for (int i = 0; i < subclasses.size(); i++) {
                int pos = (int)(offset / 8);
                ArgumentClass newClass = classes.get(i + pos).merge(subclasses.get(i));
                classes.set(i + pos, newClass);
            }

            offset += t.bitsSize() / 8;
        }

        for (int i = 0; i < classes.size(); i++) {
            ArgumentClass c = classes.get(i);

            if (c == ArgumentClass.MEMORY) {
                return createMemoryClassArray(classes.size());
            }

            if (c == ArgumentClass.X87UP) {
                if (i == 0) {
                    throw new IllegalArgumentException("Unexpected leading X87UP class");
                }

                if (classes.get(i - 1) != ArgumentClass.X87) {
                    return createMemoryClassArray(classes.size());
                }
            }
        }

        if (classes.size() > 2) {
            if (classes.get(0) != ArgumentClass.SSE) {
                return createMemoryClassArray(classes.size());
            }

            for (int i = 1; i < classes.size(); i++) {
                if (classes.get(i) != ArgumentClass.SSEUP) {
                    return createMemoryClassArray(classes.size());
                }
            }
        }

        return classes;
    }

    // TODO: handle zero length arrays
    // TODO: Handle nested structs (and primitives)
    private static List<ArgumentClass> classifyStructType(Group type) {
        long nWords = Util.alignUp((type.bitsSize() / 8), 8) / 8;
        if (nWords > MAX_AGGREGATE_REGS_SIZE) {
            return createMemoryClassArray(nWords);
        }

        ArrayList<ArgumentClass> classes = new ArrayList<>();

        for (long i = 0; i < nWords; i++) {
            classes.add(ArgumentClass.NO_CLASS);
        }

        long offset = 0;
        final int count = type.elements().size();
        for (int idx = 0; idx < count; idx++) {
            Layout t = type.elements().get(idx);
            if (t instanceof Padding) {
                continue;
            }
            // ignore zero-length array for now
            // TODO: handle zero length arrays here
            if (t instanceof Sequence) {
                if (((Sequence) t).elementsSize() == 0) {
                    continue;
                }
            }
            offset = SharedUtils.align(t, false, offset);
            List<ArgumentClass> subclasses = classifyType(t);
            if (subclasses.isEmpty()) {
                return classes;
            }

            for (int i = 0; i < subclasses.size(); i++) {
                int pos = (int)(offset / 8);
                ArgumentClass newClass = classes.get(i + pos).merge(subclasses.get(i));
                classes.set(i + pos, newClass);
            }

            // TODO: validate union strategy is sound
            if (type.kind() != Kind.UNION) {
                offset += t.bitsSize() / 8;
            }
        }

        for (int i = 0; i < classes.size(); i++) {
            ArgumentClass c = classes.get(i);

            if (c == ArgumentClass.MEMORY) {
                return createMemoryClassArray(classes.size());
            }

            if (c == ArgumentClass.X87UP) {
                if (i == 0) {
                    throw new IllegalArgumentException("Unexpected leading X87UP class");
                }

                if (classes.get(i - 1) != ArgumentClass.X87) {
                    return createMemoryClassArray(classes.size());
                }
            }
        }

        if (classes.size() > 2) {
            if (classes.get(0) != ArgumentClass.SSE) {
                return createMemoryClassArray(classes.size());
            }

            for (int i = 1; i < classes.size(); i++) {
                if (classes.get(i) != ArgumentClass.SSEUP) {
                    return createMemoryClassArray(classes.size());
                }
            }
        }

        return classes;
    }

    private static List<ArgumentClass> classifyType(Layout type) {
        try {
            if (type instanceof Value) {
                return classifyValueType((Value) type);
            } else if (type instanceof Address) {
                ArrayList<ArgumentClass> classes = new ArrayList<>();
                classes.add(ArgumentClass.INTEGER);
                return classes;
            } else if (type instanceof Sequence) {
                return classifyArrayType((Sequence) type);
            } else if (type instanceof Group) {
                return type.name().isPresent() && type.name().get().equals("LongDoubleComplex") ?
                        COMPLEX_X87_CLASSES :
                        classifyStructType((Group) type);
            } else {
                throw new IllegalArgumentException("Unhandled type " + type);
            }
        } catch (UnsupportedOperationException e) {
            System.err.println("Failed to classify layout: " + type);
            throw e;
        }
    }

    private static List<ArgumentClass> createMemoryClassArray(long n) {
        ArrayList<ArgumentClass> classes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            classes.add(ArgumentClass.MEMORY);
        }

        return classes;
    }

    static class StorageCalculator {
        private final boolean forArguments;

        private int nIntegerRegs = 0;
        private int nVectorRegs = 0;
        private int nX87Regs = 0;
        private long stackOffset = 0;

        StorageCalculator(boolean forArguments) {
            this.forArguments = forArguments;
        }

        public void addBindings(Argument arg, BiConsumer<StorageClass, ArgumentBinding> bindingConsumer) {
            ArgumentInfo info = (ArgumentInfo)arg;
            if (info.inMemory() ||
                    nIntegerRegs + info.getIntegerRegs() > (forArguments ? SysVx64ABI.MAX_INTEGER_ARGUMENT_REGISTERS : SysVx64ABI.MAX_INTEGER_RETURN_REGISTERS) ||
                    nVectorRegs + info.getVectorRegs() > (forArguments ? SysVx64ABI.MAX_VECTOR_ARGUMENT_REGISTERS : SysVx64ABI.MAX_VECTOR_RETURN_REGISTERS)) {
                // stack

                long alignment = Math.max(SharedUtils.alignment(info.layout(), true), 8);

                long newStackOffset = Util.alignUp(stackOffset, alignment);
                stackOffset = newStackOffset;

                long tmpStackOffset = stackOffset;
                for (int i = 0; i < info.getClasses().size(); i++) {
                    Storage storage = new Storage(StorageClass.STACK_ARGUMENT_SLOT, tmpStackOffset / 8, 8);
                    bindingConsumer.accept(StorageClass.STACK_ARGUMENT_SLOT, new ArgumentBinding(storage, info, i * 8));

                    if (DEBUG) {
                        System.out.println("Argument " + info.name() + " will be passed on stack at offset " + tmpStackOffset);
                    }

                    tmpStackOffset += 8;
                }

                stackOffset += info.layout().bitsSize() / 8;
            } else {
                // regs
                for (int i = 0; i < info.getClasses().size(); i++) {
                    Storage storage;

                    ArgumentClass c = info.getClasses().get(i);

                    switch (c) {
                    case INTEGER:
                        storage = new Storage(forArguments ? StorageClass.INTEGER_ARGUMENT_REGISTER : StorageClass.INTEGER_RETURN_REGISTER, nIntegerRegs++, SharedUtils.INTEGER_REGISTER_SIZE);
                        bindingConsumer.accept(storage.getStorageClass(), new ArgumentBinding(storage, info, i * 8));

                        if (DEBUG) {
                            System.out.println("Argument " + info.name() + " will be passed in register " +
                                    storageDbgHelper.getStorageName(storage));
                        }
                        break;

                    case SSE: {
                        int width = 8;

                        for (int j = i + 1; j < info.getClasses().size(); j++) {
                            if (info.getClasses().get(j) == ArgumentClass.SSEUP) {
                                width += 8;
                            }
                        }

                        if (width > 64) {
                            throw new IllegalArgumentException((width * 8) + "-bit vector arguments not supported");
                        }

                        storage = new Storage(forArguments ? StorageClass.VECTOR_ARGUMENT_REGISTER : StorageClass.VECTOR_RETURN_REGISTER,
                                nVectorRegs++, width, SharedUtils.VECTOR_REGISTER_SIZE);

                        bindingConsumer.accept(storage.getStorageClass(), new ArgumentBinding(storage, info, i * 8));

                        if (DEBUG) {
                            System.out.println("Argument " + info.name() + " will be passed in register " +
                                    storageDbgHelper.getStorageName(storage));
                        }
                        break;
                    }

                    case SSEUP:
                        break;

                    case X87: {
                        int width = 8;

                        if (i < info.getClasses().size() && info.getClasses().get(i + 1) == ArgumentClass.X87UP) {
                            width += 8;
                        }

                        assert !forArguments;

                        storage = new Storage(StorageClass.X87_RETURN_REGISTER, nX87Regs++, width, SharedUtils.X87_REGISTER_SIZE);
                        bindingConsumer.accept(storage.getStorageClass(), new ArgumentBinding(storage, info, i * 8));

                        if (DEBUG) {
                            System.out.println("Argument " + info.name() + " will be passed in register " +
                                    storageDbgHelper.getStorageName(storage));
                        }
                        break;
                    }

                    case X87UP:
                        break;

                    default:
                        throw new UnsupportedOperationException("Unhandled class " + c);
                    }
                }
            }
        }
    }
}