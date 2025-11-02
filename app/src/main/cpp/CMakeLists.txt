cmake_minimum_required(VERSION 3.18.1)

project("Nekosu")

set(CMAKE_CXX_STANDARD 17)

# Create native library
add_library(
    Nekosu
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
    Nekosu
    android
    ${log-lib}
)