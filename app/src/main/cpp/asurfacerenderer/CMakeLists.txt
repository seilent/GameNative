cmake_minimum_required(VERSION 3.22.1)

project(asurface_renderer)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

function(enable_16kb_pages target_name)
    target_link_options(${target_name} PRIVATE
            "-Wl,-z,max-page-size=16384"
            "-Wl,-z,common-page-size=16384"
    )
endfunction()

# =========================================================
# Global compiler settings
# =========================================================

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3")

# =========================================================
# ASurface Renderer library
# =========================================================

add_library(asurface_renderer SHARED
        asurface_jni.cpp
        ASurfaceRendererContext.cpp
        VsyncClock.cpp
)

target_compile_options(asurface_renderer PRIVATE
        -Wall
        -Wextra
        -fvisibility=hidden
)

target_link_libraries(asurface_renderer
        log
        android
        dl
        atomic
)

enable_16kb_pages(asurface_renderer)

# seifg frame generation engine
set(SEIFG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../libseifg)
add_subdirectory(${SEIFG_DIR} ${CMAKE_BINARY_DIR}/seifg EXCLUDE_FROM_ALL)

set(VOLK_STATIC_DEFINES VK_USE_PLATFORM_ANDROID_KHR CACHE STRING "" FORCE)

# Host effects shaders (FSR/color/FXAA pass)
add_custom_command(
    OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/host_effects_vert.h
    COMMAND glslangValidator -V --vn host_effects_vert_code -x
            -o ${CMAKE_CURRENT_BINARY_DIR}/host_effects_vert.h
            ${CMAKE_CURRENT_SOURCE_DIR}/shaders/host_effects.vert
    DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/shaders/host_effects.vert
)

add_custom_command(
    OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/host_effects_frag.h
    COMMAND glslangValidator -V --vn host_effects_frag_code -x
            -o ${CMAKE_CURRENT_BINARY_DIR}/host_effects_frag.h
            ${CMAKE_CURRENT_SOURCE_DIR}/shaders/host_effects.frag
    DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/shaders/host_effects.frag
)

target_sources(asurface_renderer PRIVATE
        seifg_host.cpp
        seifg_copy.cpp
        seifg_framegen.cpp
        host_effects.cpp
        ${CMAKE_CURRENT_BINARY_DIR}/host_effects_vert.h
        ${CMAKE_CURRENT_BINARY_DIR}/host_effects_frag.h
)

target_include_directories(asurface_renderer PRIVATE ${CMAKE_CURRENT_BINARY_DIR})

set_target_properties(asurface_renderer PROPERTIES
        CXX_STANDARD 20
        CXX_STANDARD_REQUIRED ON
)

target_compile_definitions(asurface_renderer PRIVATE VK_USE_PLATFORM_ANDROID_KHR)
target_include_directories(asurface_renderer PRIVATE ${SEIFG_DIR}/include)
target_link_libraries(asurface_renderer seifg)

# =========================================================
# Drawable library
# =========================================================


add_library(drawable SHARED
        drawable.c
)

target_compile_features(drawable PRIVATE c_std_11)

target_compile_options(drawable PRIVATE
        -O3
        -Wall
        -Wextra
        -Wno-unused-parameter
        -Rpass=loop-vectorize
        -ffast-math
)

# Enable NEON on ARM32 builds
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_options(drawable PRIVATE -mfpu=neon)
endif()

find_library(LOG_LIB log)
find_library(ANDROID_LIB android)
find_library(JNIGRAPHICS_LIB jnigraphics)

target_link_libraries(drawable
        ${LOG_LIB}
        ${ANDROID_LIB}
        ${JNIGRAPHICS_LIB}
        m
)

enable_16kb_pages(drawable)

# =========================================================
# GPUImage library
# =========================================================

add_library(gpuimage SHARED
        gpuimage.c
)

target_compile_definitions(gpuimage PRIVATE
        EGL_EGLEXT_PROTOTYPES
        GL_GLEXT_PROTOTYPES
)

target_compile_options(gpuimage PRIVATE
        -Wall
        -Wextra
)

target_link_libraries(gpuimage
        log
        android
        EGL
        GLESv2
)

enable_16kb_pages(gpuimage)
