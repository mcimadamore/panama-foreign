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
    
    static final Set<String> symbols = Set.of(
        "abort",	// stdlib.h
        "abs",	// stdlib.h
        "acos",	// math.h
        "asctime",	// time.h
        "asctime_r",	// time.h
        "asin",	// math.h
        "assert",	// assert.h
        "atan",	// math.h
        "atan2",	// math.h
        "atexit",	// stdlib.h
        "atof",	// stdlib.h
        "atoi",	// stdlib.h
        "atol",	// stdlib.h
        "bsearch",	// stdlib.h
        "btowc",	// stdio.h // wchar.h
        "calloc",	// stdlib.h
        "catclose6",	// types.h
        "catgets6",	// types.h
        "catopen6",	// types.h
        "ceil",	// math.h
        "clearerr",	// stdio.h
        "clock",	// time.h
        "cos",	// math.h
        "cosh",	// math.h
        "ctime",	// time.h
        "ctime64",	// time.h
        "ctime_r",	// time.h
        "ctime64_r",	// time.h
        "difftime",	// time.h
        "difftime64",	// time.h
        "div",	// stdlib.h
        "erf",	// math.h
        "erfc",	// math.h
        "exit",	// stdlib.h
        "exp",	// math.h
        "fabs",	// math.h
        "fclose",	// stdio.h
        "fdopen5",	// stdio.h
        "feof",	// stdio.h
        "ferror",	// stdio.h
        "fflush1",	// stdio.h
        "fgetc1",	// stdio.h
        "fgetpos1",	// stdio.h
        "fgets1",	// stdio.h
        "fgetwc6",	// stdio.h // wchar.h
        "fgetws6",	// stdio.h // wchar.h
        "fileno5",	// stdio.h
        "floor",	// math.h
        "fmod",	// math.h
        "fopen",	// stdio.h
        "fprintf",	// stdio.h
        "fputc1",	// stdio.h
        "fputs1",	// stdio.h
        "fputwc6",	// stdio.h // wchar.h
        "fputws6",	// stdio.h // wchar.h
        "fread",	// stdio.h
        "free",	// stdlib.h
        "freopen",	// stdio.h
        "frexp",	// math.h
        "fscanf",	// stdio.h
        "fseek1",	// stdio.h
        "fsetpos1",	// stdio.h
        "ftell1",	// stdio.h
        "fwide6",	// stdio.h // wchar.h
        "fwprintf6",	// stdio.h // wchar.h
        "fwrite",	// stdio.h
        "fwscanf6",	// stdio.h // wchar.h
        "gamma",	// math.h
        "getc1",	// stdio.h
        "getchar1",	// stdio.h
        "getenv",	// stdlib.h
        "gets",	// stdio.h
        "getwc6",	// stdio.h // wchar.h
        "getwchar6",	// wchar.h
        "gmtime",	// time.h
        "gmtime64",	// time.h
        "gmtime_r",	// time.h
        "gmtime64_r",	// time.h
        "hypot",	// math.h
        "isalnum",	// ctype.h
        "isalpha",	// ctype.h
        "isascii4",	// ctype.h
        "isblank",	// ctype.h
        "iscntrl",	// ctype.h
        "isdigit",	// ctype.h
        "isgraph",	// ctype.h
        "islower",	// ctype.h
        "isprint",	// ctype.h
        "ispunct",	// ctype.h
        "isspace",	// ctype.h
        "isupper",	// ctype.h
        "iswalnum4",	// wctype.h
        "iswalpha4",	// wctype.h
        "iswblank4",	// wctype.h
        "iswcntrl4",	// wctype.h
        "iswctype4",	// wctype.h
        "iswdigit4",	// wctype.h
        "iswgraph4",	// wctype.h
        "iswlower4",	// wctype.h
        "iswprint4",	// wctype.h
        "iswpunct4",	// wctype.h
        "iswspace4",	// wctype.h
        "iswupper4",	// wctype.h
        "iswxdigit4",	// wctype.h
        "isxdigit4",	// wctype.h
        "j0",	// math.h
        "j1",	// math.h
        "jn",	// math.h
        "labs",	// stdlib.h
        "ldexp",	// math.h
        "ldiv",	// stdlib.h
        "localeconv",	// locale.h
        "localtime",	// time.h
        "localtime64",	// time.h
        "localtime_r",	// time.h
        "localtime64_r",	// time.h
        "log",	// math.h
        "log10",	// math.h
        "longjmp",	// setjmp.h
        "malloc",	// stdlib.h
        "mblen",	// stdlib.h
        "mbrlen4",	// wchar.h
        "mbrtowc4",	// wchar.h
        "mbsinit4",	// wchar.h
        "mbsrtowcs4",	// wchar.h
        "mbstowcs",	// stdlib.h
        "mbtowc",	// stdlib.h
        "memchr",	// string.h
        "memcmp",	// string.h
        "memcpy",	// string.h
        "memmove",	// string.h
        "memset",	// string.h
        "mktime",	// time.h
        "mktime64",	// time.h
        "modf",	// math.h
        "nextafter",	// math.h
        "nextafterl",	// math.h
        "nexttoward",	// math.h
        "nexttowardl",	// math.h
        "nl_langinfo4",	// langinfo.h
        "perror",	// stdio.h
        "pow",	// math.h
        "printf",	// stdio.h
        "putc1",	// stdio.h
        "putchar1",	// stdio.h
        "putenv",	// stdlib.h
        "puts",	// stdio.h
        "putwc6",	// stdio.h // wchar.h
        "putwchar6",	// wchar.h
        "qsort",	// stdlib.h
        "quantexpd32",	// math.h
        "quantexpd64",	// math.h
        "quantexpd128",	// math.h
        "quantized32",	// math.h
        "quantized64",	// math.h
        "quantized128",	// math.h
        "samequantumd32",	// math.h
        "samequantumd64",	// math.h
        "samequantumd128",	// math.h
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
        "rewind1",	// stdio.h
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
        "strfmon4",	// wchar.h
        "strftime",	// time.h
        "strlen",	// string.h
        "strncasecmp",	// strings.h
        "strncat",	// string.h
        "strncmp",	// string.h
        "strncpy",	// string.h
        "strpbrk",	// string.h
        "strptime4",	// time.h
        "strrchr",	// string.h
        "strspn",	// string.h
        "strstr",	// string.h
        "strtod",	// stdlib.h
        "strtod32",	// stdlib.h
        "strtod64",	// stdlib.h
        "strtod128",	// stdlib.h
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
        "time64",	// time.h
        "tmpfile",	// stdio.h
        "tmpnam",	// stdio.h
        "toascii",	// ctype.h
        "tolower",	// ctype.h
        "toupper",	// ctype.h
        "towctrans",	// wctype.h
        "towlower4",	// wctype.h
        "towupper4",	// wctype.h
        "ungetc1",	// stdio.h
        "ungetwc6",	// stdio.h // wchar.h
        "va_arg",	// stdarg.h
        "va_copy",	// stdarg.h
        "va_end",	// stdarg.h
        "va_start",	// stdarg.h
        "vfprintf",	// stdio.h // stdarg.h
        "vfscanf",	// stdio.h // stdarg.h
        "vfwprintf6",	// stdarg.h // stdio.h // wchar.h
        "vfwscanf",	// stdio.h // stdarg.h
        "vprintf",	// stdio.h // stdarg.h
        "vscanf",	// stdio.h // stdarg.h
        "vsprintf",	// stdio.h // stdarg.h
        "vsnprintf",	// stdio.h
        "vsscanf",	// stdio.h // stdarg.h
        "vswprintf",	// stdarg.h // wchar.h
        "vswscanf",	// stdio.h // wchar.h
        "vwprintf6",	// stdarg.h // wchar.h
        "vwscanf",	// stdio.h // wchar.h
        "wcrtomb4",	// wchar.h
        "wcscat",	// wchar.h
        "wcschr",	// wchar.h
        "wcscmp",	// wchar.h
        "wcscoll4",	// wchar.h
        "wcscpy",	// wchar.h
        "wcscspn",	// wchar.h
        "wcsftime",	// wchar.h
        "wcslen",	// wchar.h
        "wcslocaleconv",	// locale.h
        "wcsncat",	// wchar.h
        "wcsncmp",	// wchar.h
        "wcsncpy",	// wchar.h
        "wcspbrk",	// wchar.h
        "wcsptime",	// wchar.h
        "wcsrchr",	// wchar.h
        "wcsrtombs4",	// wchar.h
        "wcsspn",	// wchar.h
        "wcsstr",	// wchar.h
        "wcstod",	// wchar.h
        "wcstod32",	// wchar.h
        "wcstod64",	// wchar.h
        "wcstod128",	// wchar.h
        "wcstof",	// wchar.h
        "wcstok",	// wchar.h
        "wcstol",	// wchar.h
        "wcstold",	// wchar.h
        "wcstombs",	// stdlib.h
        "wcstoul",	// wchar.h
        "wcsxfrm4",	// wchar.h
        "wctob",	// stdarg.h // wchar.h
        "wctomb",	// stdlib.h
        "wctrans",	// wctype.h
        "wctype4",	// wchar.h
        "wcwidth",	// wchar.h
        "wmemchr",	// wchar.h
        "wmemcmp",	// wchar.h
        "wmemcpy",	// wchar.h
        "wmemmove",	// wchar.h
        "wmemset",	// wchar.h
        "wprintf6",	// wchar.h
        "wscanf6",	// wchar.h
        "y0",	// math.h
        "y1",	// math.h
        "yn"	// math.h
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
