/* config.h for Android NDK build */

/* Define if you have C99's `restrict' keyword. */
#define HAVErestrict 1

/* Define if the C compiler supports __builtin_rint */
#define HAVE___BUILTIN_RINT 1

/* Define if you have the <inttypes.h> header file. */
#define HAVE_INTTYPES_H 1

/* Define if you have the <memory.h> header file. */
#define HAVE_MEMORY_H 1

/* Define if you have the <stdint.h> header file. */
#define HAVE_STDINT_H 1

/* Define if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define if you have the <strings.h> header file. */
#define HAVE_STRINGS_H 1

/* Define if you have the <string.h> header file. */
#define HAVE_STRING_H 1

/* Define if you have the <sys/stat.h> header file. */
#define HAVE_SYS_STAT_H 1

/* Define if you have the <sys/types.h> header file. */
#define HAVE_SYS_TYPES_H 1

/* Define if you have the <unistd.h> header file. */
#define HAVE_UNISTD_H 1

/* Define if you have the ANSI C header files. */
#define STDC_HEADERS 1

/* Define if you have the <float.h> header file. */
#define HAVE_FLOAT_H 1

/* Define if you have the <limits.h> header file. */
#define HAVE_LIMITS_H 1

/* Define if you have the <malloc.h> header file. */
/* #undef HAVE_MALLOC_H */

/* Define if compiler has __builtin_alloca */
#define HAVE___BUILTIN_ALLOCA 1

/* Define if you have alloca, as a function or macro. */
#define HAVE_ALLOCA 1

/* Define if you have <alloca.h> and it should be used (not on Ultrix). */
#define HAVE_ALLOCA_H 1

/* Define if you have the IEEE Math Library */
#define HAVE_IEEEFP 1

/* IEEE 754 types for Android/Bionic */
typedef float ieee754_float32_t;
typedef double ieee754_float64_t;

/* Define if your compiler implements the inline keyword */
#define inline __inline__

/* Define to 1 if you have the `mkstemps' function. */
#define HAVE_MKSTEMPS 1

/* Define if you have MMX assembly support */
/* #undef HAVE_NASM */

/* Define if you have GTK */
/* #undef HAVE_GTK */

/* Define if you have termios */
#define HAVE_TERMIOS 1

/* Define if you have sound file support */
/* #undef HAVE_SNDFILE */

/* Define if you have libmpg123 */
/* #undef HAVE_MPGLIB */

/* Version number of package */
#define VERSION "3.100"

/* The number of bytes in type int */
#define SIZEOF_INT 4

/* The number of bytes in type long */
#define SIZEOF_LONG 4

/* Define if you have the <fcntl.h> header file. */
#define HAVE_FCNTL_H 1
