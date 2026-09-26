cmake_minimum_required(VERSION 3.15)
project(cobe_native_decoder C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(NOT CMAKE_BUILD_TYPE AND NOT CMAKE_CONFIGURATION_TYPES)
    set(CMAKE_BUILD_TYPE Release CACHE STRING "" FORCE)
endif()

set(CMAKE_C_VISIBILITY_PRESET   hidden)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)

set(TP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party")

find_program(MAKE_EXECUTABLE make REQUIRED)

set(OPENH264_DIR "${TP_DIR}/openh264")

if(CMAKE_CROSSCOMPILING AND MINGW)
    set(OPENH264_BUILD_DIR "${OPENH264_DIR}/build-win")
    set(OPENH264_MAKE_ARGS
        "OS=mingw_nt"
        "BUILDDIR=${OPENH264_BUILD_DIR}"
        "CC=${CMAKE_C_COMPILER}"
        "CXX=${CMAKE_CXX_COMPILER}"
        "AR=${CMAKE_AR}"
        "RANLIB=${CMAKE_RANLIB}"
    )
elseif(APPLE)
    set(OPENH264_BUILD_DIR "${OPENH264_DIR}/build-macos")
    set(OPENH264_MAKE_ARGS
        "OS=darwin"
        "BUILDDIR=${OPENH264_BUILD_DIR}"
    )
else()
    set(OPENH264_BUILD_DIR "${OPENH264_DIR}/build-linux")
    set(OPENH264_MAKE_ARGS
        "BUILDDIR=${OPENH264_BUILD_DIR}"
    )
endif()

set(OPENH264_STATIC_LIB "${OPENH264_BUILD_DIR}/libopenh264.a")

add_custom_command(
    OUTPUT "${OPENH264_STATIC_LIB}"
    COMMAND ${CMAKE_COMMAND} -E echo "=== Building OpenH264 (${OPENH264_BUILD_DIR}) ==="
    COMMAND ${MAKE_EXECUTABLE} -C "${OPENH264_DIR}" clean
    COMMAND ${MAKE_EXECUTABLE} -C "${OPENH264_DIR}" -j ${OPENH264_MAKE_ARGS} libopenh264.a
    COMMAND ${CMAKE_COMMAND} -E make_directory "${OPENH264_BUILD_DIR}"
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
            "${OPENH264_DIR}/libopenh264.a"
            "${OPENH264_STATIC_LIB}"
    COMMENT "Building OpenH264"
    WORKING_DIRECTORY "${OPENH264_DIR}"
    VERBATIM
    USES_TERMINAL
)

add_custom_target(openh264_build DEPENDS "${OPENH264_STATIC_LIB}")

add_library(openh264 STATIC IMPORTED GLOBAL)
set_target_properties(openh264 PROPERTIES
    IMPORTED_LOCATION "${OPENH264_STATIC_LIB}"
    INTERFACE_INCLUDE_DIRECTORIES "${OPENH264_DIR}/codec/api/wels"
)
add_dependencies(openh264 openh264_build)

if(UNIX AND NOT APPLE)
    set(OPENH264_SYS_LIBS pthread dl m)
else()
    set(OPENH264_SYS_LIBS "")
endif()

add_library(pl_mpeg STATIC "${TP_DIR}/pl_mpeg/pl_mpeg.c")
target_include_directories(pl_mpeg PUBLIC "${TP_DIR}/pl_mpeg")

add_library(minimp4 STATIC "${TP_DIR}/minimp4/minimp4.c")
target_include_directories(minimp4 PUBLIC "${TP_DIR}/minimp4")

add_library(stb_image STATIC "${TP_DIR}/stb/stb_image.c")
target_include_directories(stb_image PUBLIC "${TP_DIR}/stb")

set(JNI_INCLUDE_DIRS "")

if(DEFINED ENV{CROSS_JNI_INCLUDE_DIRS} AND NOT "$ENV{CROSS_JNI_INCLUDE_DIRS}" STREQUAL "")
    message(STATUS "Using cross-compile JNI headers: $ENV{CROSS_JNI_INCLUDE_DIRS}")
    string(REPLACE ":" ";" JNI_INCLUDE_DIRS "$ENV{CROSS_JNI_INCLUDE_DIRS}")
elseif(DEFINED ENV{JAVA_HOME})
    set(_jni_inc "$ENV{JAVA_HOME}/include")
    if(WIN32)
        set(JNI_INCLUDE_DIRS "${_jni_inc}" "${_jni_inc}/win32")
    elseif(APPLE)
        set(JNI_INCLUDE_DIRS "${_jni_inc}" "${_jni_inc}/darwin")
    else()
        set(JNI_INCLUDE_DIRS "${_jni_inc}" "${_jni_inc}/linux")
    endif()
else()
    find_package(JNI REQUIRED)
endif()

message(STATUS "JNI include dirs: ${JNI_INCLUDE_DIRS}")

add_library(cobe_native_decoder SHARED native_decoder.cpp)

target_include_directories(cobe_native_decoder PRIVATE
    ${JNI_INCLUDE_DIRS}
    "${OPENH264_DIR}/codec/api/wels"
)

target_link_libraries(cobe_native_decoder PRIVATE
    openh264 pl_mpeg minimp4 stb_image
    ${OPENH264_SYS_LIBS}
)
add_dependencies(cobe_native_decoder openh264_build)

if(WIN32)
    set_target_properties(cobe_native_decoder PROPERTIES PREFIX "")
    if(MINGW)
        target_link_options(cobe_native_decoder PRIVATE
            -static-libgcc -static-libstdc++
            -Wl,-Bstatic -lstdc++ -lpthread -Wl,-Bdynamic
        )
    endif()
endif()