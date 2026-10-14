cmake_minimum_required(VERSION 3.18.1)

project("xyrecorder")

# Set optimization flags
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -ffast-math")

# Include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/libmp3lame
)

# LAME source files (only the essential ones for encoding)
set(LAME_SOURCES
    libmp3lame/bitstream.c
    libmp3lame/encoder.c
    libmp3lame/fft.c
    libmp3lame/gain_analysis.c
    libmp3lame/id3tag.c
    libmp3lame/lame.c
    libmp3lame/newmdct.c
    libmp3lame/presets.c
    libmp3lame/psymodel.c
    libmp3lame/quantize.c
    libmp3lame/quantize_pvt.c
    libmp3lame/reservoir.c
    libmp3lame/set_get.c
    libmp3lame/tables.c
    libmp3lame/takehiro.c
    libmp3lame/util.c
    libmp3lame/VbrTag.c
    libmp3lame/vbrquantize.c
    libmp3lame/version.c
)

# Build LAME as static library
add_library(mp3lame STATIC ${LAME_SOURCES})

# Build JNI wrapper as shared library
add_library(mp3lame_wrapper SHARED mp3lame_wrapper.c)

# Link wrapper with LAME library
target_link_libraries(mp3lame_wrapper mp3lame log)

# Set compile definitions for LAME
target_compile_definitions(mp3lame PRIVATE HAVE_CONFIG_H)
