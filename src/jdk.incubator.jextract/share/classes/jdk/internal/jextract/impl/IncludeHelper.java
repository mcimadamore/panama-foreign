/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.jextract.impl;

import jdk.incubator.jextract.Declaration;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

public class IncludeHelper {

    public enum IncludeKind {
        MACRO,
        VAR,
        FUNCTION,
        TYPEDEF,
        STRUCT,
        UNION;

        public String optionName() {
            return "include-" + name().toLowerCase();
        }
    }

    private final EnumMap<IncludeKind, Set<String>> includesSymbolNamesByKind = new EnumMap<>(IncludeKind.class);
    private final Set<Declaration> usedDeclarations = new HashSet<>();
    private final boolean collectUsages;

    public IncludeHelper(boolean collectUsages) {
        this.collectUsages = collectUsages;
    }

    public void addSymbol(IncludeKind kind, String symbolName) {
        Set<String> names = includesSymbolNamesByKind.computeIfAbsent(kind, (_unused) -> new HashSet<>());
        names.add(symbolName);
    }

    public boolean isIncluded(Declaration.Variable variable) {
        return isIncludedInternal(IncludeKind.VAR, variable);
    }

    public boolean isIncluded(Declaration.Function function) {
        return isIncludedInternal(IncludeKind.FUNCTION, function);
    }

    public boolean isIncluded(Declaration.Constant constant) {
        return isIncludedInternal(IncludeKind.MACRO, constant);
    }

    public boolean isIncluded(Declaration.Typedef typedef) {
        return isIncludedInternal(IncludeKind.TYPEDEF, typedef);
    }

    public boolean isIncluded(Declaration.Scoped scoped) {
        IncludeKind kind = switch (scoped.kind()) {
            case STRUCT -> IncludeKind.STRUCT;
            case UNION ->  IncludeKind.UNION;
            default -> throw new IllegalStateException("Cannot get here!");
        };
        return isIncludedInternal(kind, scoped);
    }

    private boolean isIncludedInternal(IncludeKind kind, Declaration declaration) {
        if (collectUsages) {
            usedDeclarations.add(declaration);
        }
        if (!isEnabled()) {
            return true;
        } else {
            Set<String> names = includesSymbolNamesByKind.computeIfAbsent(kind, (_unused) -> new HashSet<>());
            return names.contains(declaration.name());
        }
    }

    public boolean isEnabled() {
        return includesSymbolNamesByKind.size() > 0;
    }
}
