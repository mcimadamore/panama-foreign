/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;
import jdk.incubator.foreign.CSupport;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import static org.testng.Assert.assertEquals;
import static test.jextract.test8253102.test8253102_h.*;

/*
 * @test id=classes
 * @bug 8253102
 * @summary jextract generates uncompilable code for no argument C function
 * @library ..
 * @modules jdk.incubator.jextract
 * @run driver JtregJextract -l Test8253102 -t test.jextract.test8253102 -- test8253102.h
 * @run testng/othervm -Dforeign.restricted=permit LibTest8253102Test
 */
/*
 * @test id=sources
 * @bug 8253102
 * @summary jextract generates uncompilable code for no argument C function
 * @library ..
 * @modules jdk.incubator.jextract
 * @run driver JtregJextractSources -l Test8253102 -t test.jextract.test8253102 -- test8253102.h
 * @run testng/othervm -Dforeign.restricted=permit LibTest8253102Test
 */
public class LibTest8253102Test {
    @Test
    public void test() {
        MemoryAddress addr = make(14, 99);
        MemorySegment seg = Point.ofAddressRestricted(addr);
        assertEquals(Point.x$get(seg), 14);
        assertEquals(Point.y$get(seg), 99);
        CSupport.freeMemoryRestricted(addr);
    }
}
