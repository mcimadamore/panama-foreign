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

package jdk.internal.foreign;

import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class StdLibC implements LibraryLookup {

    private StdLibC() { }

    final static StdLibC INSTANCE = new StdLibC();
    
    public static final Set<String> symbols = Set.of(
        "abort",	// stdlib.h
        "abs",	// stdlib.h
        "acos",	// math.h
        "asctime",	// time.h
        "asctime_r",	// time.h
        "asin",	// math.h
        "atan",	// math.h
        "atan2",	// math.h
        //"atexit",	// stdlib.h
        "atof",	// stdlib.h
        "atoi",	// stdlib.h
        "atol",	// stdlib.h
        "bsearch",	// stdlib.h
        "btowc",	// stdio.h // wchar.h
        "calloc",	// stdlib.h
        "catclose",	// types.h
        "catgets",	// types.h
        "catopen",	// types.h
        "ceil",	// math.h
        "clearerr",	// stdio.h
        "clock",	// time.h
        "cos",	// math.h
        "cosh",	// math.h
        "ctime",	// time.h
        "difftime",	// time.h
        "div",	// stdlib.h
        "erf",	// math.h
        "erfc",	// math.h
        "exit",	// stdlib.h
        "exp",	// math.h
        "fabs",	// math.h
        "fclose",	// stdio.h
        "fdopen",	// stdio.h
        "feof",	// stdio.h
        "ferror",	// stdio.h
        "fflush",	// stdio.h
        "fgetc",	// stdio.h
        "fgetpos",	// stdio.h
        "fgets",	// stdio.h
        "fgetwc",	// stdio.h // wchar.h
        "fgetws",	// stdio.h // wchar.h
        "fileno",	// stdio.h
        "floor",	// math.h
        "fmod",	// math.h
        "fopen",	// stdio.h
        "fprintf",	// stdio.h
        "fputc",	// stdio.h
        "fputs",	// stdio.h
        "fputwc",	// stdio.h // wchar.h
        "fputws",	// stdio.h // wchar.h
        "fread",	// stdio.h
        "free",	// stdlib.h
        "freopen",	// stdio.h
        "frexp",	// math.h
        "fscanf",	// stdio.h
        "fseek",	// stdio.h
        "fsetpos",	// stdio.h
        "ftell",	// stdio.h
        "fwide",	// stdio.h // wchar.h
        "fwprintf",	// stdio.h // wchar.h
        "fwrite",	// stdio.h
        "fwscanf",	// stdio.h // wchar.h
        "gamma",	// math.h
        "getc",	// stdio.h
        "getchar",	// stdio.h
        "getenv",	// stdlib.h
        "gets",	// stdio.h
        "getwc",	// stdio.h // wchar.h
        "getwchar",	// wchar.h
        "gmtime",	// time.h
        "hypot",	// math.h
        "isalnum",	// ctype.h
        "isalpha",	// ctype.h
        "isascii",	// ctype.h
        "isblank",	// ctype.h
        "iscntrl",	// ctype.h
        "isdigit",	// ctype.h
        "isgraph",	// ctype.h
        "islower",	// ctype.h
        "isprint",	// ctype.h
        "ispunct",	// ctype.h
        "isspace",	// ctype.h
        "isupper",	// ctype.h
        "iswalnum",	// wctype.h
        "iswalpha",	// wctype.h
        "iswblank",	// wctype.h
        "iswcntrl",	// wctype.h
        "iswctype",	// wctype.h
        "iswdigit",	// wctype.h
        "iswgraph",	// wctype.h
        "iswlower",	// wctype.h
        "iswprint",	// wctype.h
        "iswpunct",	// wctype.h
        "iswspace",	// wctype.h
        "iswupper",	// wctype.h
        "iswxdigit",	// wctype.h
        "isxdigit",	// wctype.h
        "labs",	// stdlib.h
        "ldexp",	// math.h
        "ldiv",	// stdlib.h
        "localeconv",	// locale.h
        "localtime",	// time.h
        "log",	// math.h
        "log10",	// math.h
        "longjmp",	// setjmp.h
        "malloc",	// stdlib.h
        "mblen",	// stdlib.h
        "mbrlen",	// wchar.h
        "mbrtowc",	// wchar.h
        "mbsinit",	// wchar.h
        "mbsrtowcs",	// wchar.h
        "mbstowcs",	// stdlib.h
        "mbtowc",	// stdlib.h
        "memchr",	// string.h
        "memcmp",	// string.h
        "memcpy",	// string.h
        "memmove",	// string.h
        "memset",	// string.h
        "mktime",	// time.h
        "modf",	// math.h
        "nextafter",	// math.h
        "nextafterl",	// math.h
        "nexttoward",	// math.h
        "nexttowardl",	// math.h
        "nl_langinfo",	// langinfo.h
        "perror",	// stdio.h
        "pow",	// math.h
        "printf",	// stdio.h
        "putc",	// stdio.h
        "putchar",	// stdio.h
        "putenv",	// stdlib.h
        "puts",	// stdio.h
        "putwc",	// stdio.h // wchar.h
        "putwchar",	// wchar.h
        "qsort",	// stdlib.h
        "raise",	// signal.h
        "rand",	// stdlib.h
        "rand_r",	// stdlib.h
        "realloc",	// stdlib.h
        "regcomp",	// regex.h
        "regerror",	// regex.h
        "regexec",	// regex.h
        "regfree",	// regex.h
        "remove",	// stdio.h
        "rename",	// stdio.h
        "rewind",	// stdio.h
        "scanf",	// stdio.h
        "setbuf",	// stdio.h
        "setjmp",	// setjmp.h
        "setlocale",	// locale.h
        "setvbuf",	// stdio.h
        "signal",	// signal.h
        "sin",	// math.h
        "sinh",	// math.h
        "snprintf",	// stdio.h
        "sprintf",	// stdio.h
        "sqrt",	// math.h
        "srand",	// stdlib.h
        "sscanf",	// stdio.h
        "strcasecmp",	// strings.h
        "strcat",	// string.h
        "strchr",	// string.h
        "strcmp",	// string.h
        "strcoll",	// string.h
        "strcpy",	// string.h
        "strcspn",	// string.h
        "strerror",	// string.h
        "strfmon",	// wchar.h
        "strftime",	// time.h
        "strlen",	// string.h
        "strncasecmp",	// strings.h
        "strncat",	// string.h
        "strncmp",	// string.h
        "strncpy",	// string.h
        "strpbrk",	// string.h
        "strptime",	// time.h
        "strrchr",	// string.h
        "strspn",	// string.h
        "strstr",	// string.h
        "strtod",	// stdlib.h
        "strtof",	// stdlib.h
        "strtok",	// string.h
        "strtok_r",	// string.h
        "strtol",	// stdlib.h
        "strtold",	// stdlib.h
        "strtoul",	// stdlib.h
        "strxfrm",	// string.h
        "swprintf",	// wchar.h
        "swscanf",	// wchar.h
        "system",	// stdlib.h
        "tan",	// math.h
        "tanh",	// math.h
        "time",	// time.h
        "tmpfile",	// stdio.h
        "tmpnam",	// stdio.h
        "toascii",	// ctype.h
        "tolower",	// ctype.h
        "toupper",	// ctype.h
        "towctrans",	// wctype.h
        "towlower",	// wctype.h
        "towupper",	// wctype.h
        "ungetc",	// stdio.h
        "ungetwc",	// stdio.h // wchar.h
        "vfprintf",	// stdio.h // stdarg.h
        "vfscanf",	// stdio.h // stdarg.h
        "vfwprintf",	// stdarg.h // stdio.h // wchar.h
        "vfwscanf",	// stdio.h // stdarg.h
        "vprintf",	// stdio.h // stdarg.h
        "vscanf",	// stdio.h // stdarg.h
        "vsprintf",	// stdio.h // stdarg.h
        "vsnprintf",	// stdio.h
        "vsscanf",	// stdio.h // stdarg.h
        "vswprintf",	// stdarg.h // wchar.h
        "vswscanf",	// stdio.h // wchar.h
        "vwprintf",	// stdarg.h // wchar.h
        "vwscanf",	// stdio.h // wchar.h
        "wctrans",	// wctype.h
        "wctype"	// wchar.h
    );

    final LibraryLookup lookup = switch (CABI.current()) {
        case SysV, AArch64 -> LibrariesHelper.getDefaultLibrary();
        case Win64 -> LibraryLookup.ofPath(Path.of(System.getenv("SystemRoot"), "System32", "msvcrt.dll")); // do not depend on java.library.path!
    };

    @Override
    public Optional<MemoryAddress> lookup(String name) {
        Objects.requireNonNull(name);
        return (symbols.contains(name)) ?
                lookup.lookup(name) : Optional.empty();
    }

    @Override
    public Optional<MemorySegment> lookup(String name, MemoryLayout layout) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(layout);
        return (symbols.contains(name)) ?
                lookup.lookup(name, layout) : Optional.empty();
    }

    public static StdLibC getInstance() {
        return INSTANCE;
    }
}
