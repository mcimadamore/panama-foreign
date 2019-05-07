/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.foreign.Libraries;
import java.foreign.Library;
import java.lang.invoke.MethodHandles;
import org.testng.annotations.Test;
import test.jextract.asmsymbol.libAsmSymbol_h;

import static org.testng.Assert.assertEquals;
/*
 * @test
 * @requires os.family != "windows"
 * @library ..
 * @run driver JtregJextract -C -DADD -t test.jextract.asmsymbol -- libAsmSymbol.h
 * @run testng/othervm Test8223105A
 */
public class Test8223105A {
    static final libAsmSymbol_h libAsmSymbol;
    static {
        Library lib = Libraries.loadLibrary(MethodHandles.lookup(), "AsmSymbol");
        libAsmSymbol = Libraries.bind(libAsmSymbol_h.class, lib);
    }

    @Test
    public void checkGlobalVar() {
        assertEquals(1, libAsmSymbol.foo$get());
    }

    @Test
    public void checkGlobalFunction() {
        assertEquals(3, libAsmSymbol.func(1, 2));
    }
}
