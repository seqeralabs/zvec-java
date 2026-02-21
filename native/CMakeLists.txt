cmake_minimum_required(VERSION 3.20)
project(zvec_c C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)

# Path to the zvec project root — adjust as needed
if(NOT DEFINED ZVEC_ROOT)
    set(ZVEC_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../zvec")
endif()

if(NOT DEFINED ZVEC_BUILD_DIR)
    set(ZVEC_BUILD_DIR "${ZVEC_ROOT}/build")
endif()

set(ZVEC_LIB_DIR "${ZVEC_BUILD_DIR}/lib")
set(ZVEC_EXT_LIB_DIR "${ZVEC_BUILD_DIR}/external/usr/local/lib")

add_library(zvec_c SHARED zvec_c.cpp)

target_include_directories(zvec_c PRIVATE
    ${ZVEC_ROOT}/src
    ${ZVEC_ROOT}/src/include
    ${ZVEC_BUILD_DIR}/external/usr/local/include
)

# Link against zvec libraries — mirrors the Python binding CMakeLists.txt
# The core algorithm modules must be whole-archive linked to register their factories.
if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    target_link_libraries(zvec_c PRIVATE
        -Wl,--whole-archive
        ${ZVEC_LIB_DIR}/libcore_knn_flat.a
        ${ZVEC_LIB_DIR}/libcore_knn_flat_sparse.a
        ${ZVEC_LIB_DIR}/libcore_knn_hnsw.a
        ${ZVEC_LIB_DIR}/libcore_knn_hnsw_sparse.a
        ${ZVEC_LIB_DIR}/libcore_knn_ivf.a
        ${ZVEC_LIB_DIR}/libcore_knn_cluster.a
        ${ZVEC_LIB_DIR}/libcore_mix_reducer.a
        ${ZVEC_LIB_DIR}/libcore_metric.a
        ${ZVEC_LIB_DIR}/libcore_utility.a
        ${ZVEC_LIB_DIR}/libcore_quantizer.a
        -Wl,--no-whole-archive
        -Wl,--start-group
        ${ZVEC_LIB_DIR}/libzvec_db.a
        ${ZVEC_LIB_DIR}/libzvec_core.a
        ${ZVEC_LIB_DIR}/libzvec_index.a
        ${ZVEC_LIB_DIR}/libzvec_common.a
        ${ZVEC_LIB_DIR}/libzvec_sqlengine.a
        ${ZVEC_LIB_DIR}/libzvec_ailego.a
        ${ZVEC_LIB_DIR}/libzvec_proto.a
        ${ZVEC_LIB_DIR}/libcore_interface.a
        ${ZVEC_LIB_DIR}/libcore_framework.a
        ${ZVEC_EXT_LIB_DIR}/libglog.a
        ${ZVEC_EXT_LIB_DIR}/libgflags_nothreads.a
        ${ZVEC_EXT_LIB_DIR}/libroaring.a
        ${ZVEC_EXT_LIB_DIR}/librocksdb.a
        ${ZVEC_EXT_LIB_DIR}/libantlr4-runtime.a
        ${ZVEC_EXT_LIB_DIR}/libprotobuf.a
        ${ZVEC_EXT_LIB_DIR}/libparquet.a
        ${ZVEC_EXT_LIB_DIR}/libarrow.a
        ${ZVEC_EXT_LIB_DIR}/libarrow_compute.a
        ${ZVEC_EXT_LIB_DIR}/libarrow_dataset.a
        ${ZVEC_EXT_LIB_DIR}/libarrow_acero.a
        ${ZVEC_EXT_LIB_DIR}/libarrow_bundled_dependencies.a
        ${ZVEC_EXT_LIB_DIR}/liblz4.a
        -Wl,--end-group
    )
elseif(APPLE)
    target_link_libraries(zvec_c PRIVATE
        -Wl,-force_load,${ZVEC_LIB_DIR}/libcore_knn_flat.a
        -Wl,-force_load,${ZVEC_LIB_DIR}/libcore_knn_flat_sparse.a
        -Wl,-force_load,${ZVEC_LIB_DIR}/libcore_knn_hnsw.a
        -Wl,-force_load,${ZVEC_LIB_DIR}/libcore_knn_hnsw_sparse.a
        -Wl,-force_load,${ZVEC_LIB_DIR}/libcore_knn_ivf.a
        -Wl,-force_load,${ZVEC_LIB_DIR}/libcore_knn_cluster.a
        -Wl,-force_load,${ZVEC_LIB_DIR}/libcore_mix_reducer.a
        -Wl,-force_load,${ZVEC_LIB_DIR}/libcore_metric.a
        -Wl,-force_load,${ZVEC_LIB_DIR}/libcore_utility.a
        -Wl,-force_load,${ZVEC_LIB_DIR}/libcore_quantizer.a
        ${ZVEC_LIB_DIR}/libzvec_db.a
        ${ZVEC_LIB_DIR}/libzvec_core.a
        ${ZVEC_LIB_DIR}/libzvec_index.a
        ${ZVEC_LIB_DIR}/libzvec_common.a
        ${ZVEC_LIB_DIR}/libzvec_sqlengine.a
        ${ZVEC_LIB_DIR}/libzvec_ailego.a
        ${ZVEC_LIB_DIR}/libzvec_proto.a
        ${ZVEC_LIB_DIR}/libcore_interface.a
        ${ZVEC_LIB_DIR}/libcore_framework.a
        ${ZVEC_EXT_LIB_DIR}/libglog.a
        ${ZVEC_EXT_LIB_DIR}/libgflags_nothreads.a
        ${ZVEC_EXT_LIB_DIR}/libroaring.a
        ${ZVEC_EXT_LIB_DIR}/librocksdb.a
        ${ZVEC_EXT_LIB_DIR}/libantlr4-runtime.a
        ${ZVEC_EXT_LIB_DIR}/libprotobuf.a
        ${ZVEC_EXT_LIB_DIR}/libparquet.a
        ${ZVEC_EXT_LIB_DIR}/libarrow.a
        ${ZVEC_EXT_LIB_DIR}/libarrow_compute.a
        ${ZVEC_EXT_LIB_DIR}/libarrow_dataset.a
        ${ZVEC_EXT_LIB_DIR}/libarrow_acero.a
        ${ZVEC_EXT_LIB_DIR}/libarrow_bundled_dependencies.a
        ${ZVEC_EXT_LIB_DIR}/liblz4.a
    )
endif()

# Ensure C linkage visibility
set_target_properties(zvec_c PROPERTIES
    C_VISIBILITY_PRESET default
    CXX_VISIBILITY_PRESET default
    POSITION_INDEPENDENT_CODE ON
)
