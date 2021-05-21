/*
 *  Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 */

// Generated by jextract

package jdk.internal.clang.libclang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import jdk.incubator.foreign.*;
import static jdk.incubator.foreign.CLinker.*;
class constants$6 {

    static final FunctionDescriptor clang_getCursorKind$FUNC = FunctionDescriptor.of(C_INT,
        MemoryLayout.structLayout(
            C_INT.withName("kind"),
            C_INT.withName("xdata"),
            MemoryLayout.sequenceLayout(3, C_POINTER).withName("data")
        )
    );
    static final MethodHandle clang_getCursorKind$MH = RuntimeHelper.downcallHandle(
        Index_h.LIBRARIES, "clang_getCursorKind",
        "(Ljdk/incubator/foreign/MemorySegment;)I",
        constants$6.clang_getCursorKind$FUNC, false
    );
    static final FunctionDescriptor clang_isDeclaration$FUNC = FunctionDescriptor.of(C_INT,
        C_INT
    );
    static final MethodHandle clang_isDeclaration$MH = RuntimeHelper.downcallHandle(
        Index_h.LIBRARIES, "clang_isDeclaration",
        "(I)I",
        constants$6.clang_isDeclaration$FUNC, false
    );
    static final FunctionDescriptor clang_isAttribute$FUNC = FunctionDescriptor.of(C_INT,
        C_INT
    );
    static final MethodHandle clang_isAttribute$MH = RuntimeHelper.downcallHandle(
        Index_h.LIBRARIES, "clang_isAttribute",
        "(I)I",
        constants$6.clang_isAttribute$FUNC, false
    );
    static final FunctionDescriptor clang_isInvalid$FUNC = FunctionDescriptor.of(C_INT,
        C_INT
    );
    static final MethodHandle clang_isInvalid$MH = RuntimeHelper.downcallHandle(
        Index_h.LIBRARIES, "clang_isInvalid",
        "(I)I",
        constants$6.clang_isInvalid$FUNC, false
    );
    static final FunctionDescriptor clang_isPreprocessing$FUNC = FunctionDescriptor.of(C_INT,
        C_INT
    );
    static final MethodHandle clang_isPreprocessing$MH = RuntimeHelper.downcallHandle(
        Index_h.LIBRARIES, "clang_isPreprocessing",
        "(I)I",
        constants$6.clang_isPreprocessing$FUNC, false
    );
    static final FunctionDescriptor clang_getCursorLanguage$FUNC = FunctionDescriptor.of(C_INT,
        MemoryLayout.structLayout(
            C_INT.withName("kind"),
            C_INT.withName("xdata"),
            MemoryLayout.sequenceLayout(3, C_POINTER).withName("data")
        )
    );
    static final MethodHandle clang_getCursorLanguage$MH = RuntimeHelper.downcallHandle(
        Index_h.LIBRARIES, "clang_getCursorLanguage",
        "(Ljdk/incubator/foreign/MemorySegment;)I",
        constants$6.clang_getCursorLanguage$FUNC, false
    );
}


