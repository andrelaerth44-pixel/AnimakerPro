cmake_minimum_required(VERSION 3.22.1)
project(animakerpro_native)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(animakerpro_native SHARED NativeCanvasEngine.cpp)

target_compile_options(animakerpro_native PRIVATE -O2 -ffast-math -fno-exceptions -fno-rtti)
target_link_libraries(animakerpro_native android log GLESv3 EGL)
