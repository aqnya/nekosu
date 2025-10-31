cmake_minimum_required(VERSION 3.18.1)

project("nzhelper")

set(CMAKE_CXX_STANDARD 17)

# Create native library
add_library(
    nzhelper
    SHARED
    native-lib.cpp
)

# Find required libraries
find_library(
    log-lib
    log
)

# Link libraries
target_link_libraries(
    nzhelper
    android
    ${log-lib}
)