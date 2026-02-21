/*
 * Copyright 2025-present the zvec project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef ZVEC_C_H
#define ZVEC_C_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ========== Status codes ========== */
#define ZVEC_OK                  0
#define ZVEC_NOT_FOUND           1
#define ZVEC_ALREADY_EXISTS      2
#define ZVEC_INVALID_ARGUMENT    3
#define ZVEC_PERMISSION_DENIED   4
#define ZVEC_FAILED_PRECONDITION 5
#define ZVEC_RESOURCE_EXHAUSTED  6
#define ZVEC_UNAVAILABLE         7
#define ZVEC_INTERNAL_ERROR      8
#define ZVEC_NOT_SUPPORTED       9
#define ZVEC_UNKNOWN            10

/* ========== DataType constants ========== */
#define ZVEC_DATATYPE_UNDEFINED         0
#define ZVEC_DATATYPE_BINARY            1
#define ZVEC_DATATYPE_STRING            2
#define ZVEC_DATATYPE_BOOL              3
#define ZVEC_DATATYPE_INT32             4
#define ZVEC_DATATYPE_INT64             5
#define ZVEC_DATATYPE_UINT32            6
#define ZVEC_DATATYPE_UINT64            7
#define ZVEC_DATATYPE_FLOAT             8
#define ZVEC_DATATYPE_DOUBLE            9
#define ZVEC_DATATYPE_VECTOR_BINARY32  20
#define ZVEC_DATATYPE_VECTOR_BINARY64  21
#define ZVEC_DATATYPE_VECTOR_FP16      22
#define ZVEC_DATATYPE_VECTOR_FP32      23
#define ZVEC_DATATYPE_VECTOR_FP64      24
#define ZVEC_DATATYPE_VECTOR_INT4      25
#define ZVEC_DATATYPE_VECTOR_INT8      26
#define ZVEC_DATATYPE_VECTOR_INT16     27
#define ZVEC_DATATYPE_SPARSE_VECTOR_FP16 30
#define ZVEC_DATATYPE_SPARSE_VECTOR_FP32 31
#define ZVEC_DATATYPE_ARRAY_BINARY     40
#define ZVEC_DATATYPE_ARRAY_STRING     41
#define ZVEC_DATATYPE_ARRAY_BOOL       42
#define ZVEC_DATATYPE_ARRAY_INT32      43
#define ZVEC_DATATYPE_ARRAY_INT64      44
#define ZVEC_DATATYPE_ARRAY_UINT32     45
#define ZVEC_DATATYPE_ARRAY_UINT64     46
#define ZVEC_DATATYPE_ARRAY_FLOAT      47
#define ZVEC_DATATYPE_ARRAY_DOUBLE     48

/* ========== IndexType constants ========== */
#define ZVEC_INDEX_UNDEFINED  0
#define ZVEC_INDEX_HNSW       1
#define ZVEC_INDEX_IVF        3
#define ZVEC_INDEX_FLAT       4
#define ZVEC_INDEX_INVERT    10

/* ========== MetricType constants ========== */
#define ZVEC_METRIC_UNDEFINED  0
#define ZVEC_METRIC_L2         1
#define ZVEC_METRIC_IP         2
#define ZVEC_METRIC_COSINE     3
#define ZVEC_METRIC_MIPSL2     4

/* ========== QuantizeType constants ========== */
#define ZVEC_QUANTIZE_UNDEFINED 0
#define ZVEC_QUANTIZE_FP16      1
#define ZVEC_QUANTIZE_INT8      2
#define ZVEC_QUANTIZE_INT4      3

/* ========== LogLevel constants ========== */
#define ZVEC_LOG_DEBUG  0
#define ZVEC_LOG_INFO   1
#define ZVEC_LOG_WARN   2
#define ZVEC_LOG_ERROR  3
#define ZVEC_LOG_FATAL  4

/* ========== Opaque handle types ========== */
typedef struct zvec_collection_s*       zvec_collection_t;
typedef struct zvec_collection_schema_s* zvec_collection_schema_t;
typedef struct zvec_field_schema_s*     zvec_field_schema_t;
typedef struct zvec_index_params_s*     zvec_index_params_t;
typedef struct zvec_query_params_s*     zvec_query_params_t;
typedef struct zvec_doc_s*              zvec_doc_t;
typedef struct zvec_doc_list_s*         zvec_doc_list_t;
typedef struct zvec_doc_map_s*          zvec_doc_map_t;
typedef struct zvec_write_results_s*    zvec_write_results_t;
typedef struct zvec_string_array_s*     zvec_string_array_t;
typedef struct zvec_vector_query_s*     zvec_vector_query_t;

/* ========== Error message ========== */
const char* zvec_last_error_message(void);

/* ========== Memory management ========== */
void zvec_free_string(char* str);

/* ========== Global config ========== */
int zvec_global_config_init(uint32_t log_level, uint32_t query_threads, uint32_t optimize_threads);

/* ========== Collection lifecycle ========== */
int zvec_collection_create_and_open(const char* path,
                                     zvec_collection_schema_t schema,
                                     int read_only, int enable_mmap, uint32_t max_buffer_size,
                                     zvec_collection_t* out);

int zvec_collection_open(const char* path,
                          int read_only, int enable_mmap, uint32_t max_buffer_size,
                          zvec_collection_t* out);

int zvec_collection_close(zvec_collection_t coll);

int zvec_collection_destroy(zvec_collection_t coll);

int zvec_collection_flush(zvec_collection_t coll);

/* ========== Collection properties ========== */
int zvec_collection_path(zvec_collection_t coll, char** out);

int zvec_collection_stats(zvec_collection_t coll, uint64_t* doc_count);

int zvec_collection_schema(zvec_collection_t coll, zvec_collection_schema_t* out);

/* ========== CollectionSchema ========== */
int zvec_collection_schema_create(const char* name, zvec_collection_schema_t* out);

int zvec_collection_schema_add_field(zvec_collection_schema_t schema, zvec_field_schema_t field);

int zvec_collection_schema_destroy(zvec_collection_schema_t schema);

int zvec_collection_schema_name(zvec_collection_schema_t schema, char** out);

int zvec_collection_schema_field_count(zvec_collection_schema_t schema, uint32_t* out);

int zvec_collection_schema_field_at(zvec_collection_schema_t schema, uint32_t index, zvec_field_schema_t* out);

int zvec_collection_schema_field_by_name(zvec_collection_schema_t schema, const char* name, zvec_field_schema_t* out);

/* ========== FieldSchema ========== */
int zvec_field_schema_create(const char* name, uint32_t data_type, int nullable,
                              zvec_index_params_t index_params,
                              zvec_field_schema_t* out);

int zvec_field_schema_create_vector(const char* name, uint32_t data_type, uint32_t dimension,
                                     int nullable, zvec_index_params_t index_params,
                                     zvec_field_schema_t* out);

int zvec_field_schema_destroy(zvec_field_schema_t fs);

int zvec_field_schema_name(zvec_field_schema_t fs, char** out);

int zvec_field_schema_data_type(zvec_field_schema_t fs, uint32_t* out);

int zvec_field_schema_dimension(zvec_field_schema_t fs, uint32_t* out);

int zvec_field_schema_nullable(zvec_field_schema_t fs, int* out);

int zvec_field_schema_index_type(zvec_field_schema_t fs, uint32_t* out);

/* ========== Index params factories ========== */
int zvec_hnsw_index_params_create(uint32_t metric_type, int m, int ef_construction,
                                   uint32_t quantize_type, zvec_index_params_t* out);

int zvec_flat_index_params_create(uint32_t metric_type, uint32_t quantize_type,
                                   zvec_index_params_t* out);

int zvec_ivf_index_params_create(uint32_t metric_type, int n_list, int n_iters,
                                  int use_soar, uint32_t quantize_type,
                                  zvec_index_params_t* out);

int zvec_invert_index_params_create(int enable_range_optimization,
                                     int enable_extended_wildcard,
                                     zvec_index_params_t* out);

int zvec_index_params_destroy(zvec_index_params_t params);

/* ========== Query params factories ========== */
int zvec_hnsw_query_params_create(int ef, float radius, int is_linear,
                                   int is_using_refiner, zvec_query_params_t* out);

int zvec_flat_query_params_create(int is_using_refiner, float scale_factor,
                                   zvec_query_params_t* out);

int zvec_ivf_query_params_create(int nprobe, int is_using_refiner, float scale_factor,
                                  zvec_query_params_t* out);

int zvec_query_params_destroy(zvec_query_params_t params);

/* ========== Index DDL ========== */
int zvec_collection_create_index(zvec_collection_t coll, const char* field_name,
                                  zvec_index_params_t index_params, int concurrency);

int zvec_collection_drop_index(zvec_collection_t coll, const char* field_name);

int zvec_collection_optimize(zvec_collection_t coll, int concurrency);

/* ========== Column DDL ========== */
int zvec_collection_add_column(zvec_collection_t coll, zvec_field_schema_t field_schema,
                                const char* expression, int concurrency);

int zvec_collection_drop_column(zvec_collection_t coll, const char* field_name);

int zvec_collection_alter_column(zvec_collection_t coll, const char* old_name,
                                  const char* new_name, zvec_field_schema_t new_schema,
                                  int concurrency);

/* ========== Doc ========== */
int zvec_doc_create(zvec_doc_t* out);

int zvec_doc_destroy(zvec_doc_t doc);

int zvec_doc_set_pk(zvec_doc_t doc, const char* pk);

int zvec_doc_get_pk(zvec_doc_t doc, char** out);

int zvec_doc_get_score(zvec_doc_t doc, float* out);

/* Field setters */
int zvec_doc_set_string(zvec_doc_t doc, const char* field, const char* value);

int zvec_doc_set_int32(zvec_doc_t doc, const char* field, int32_t value);

int zvec_doc_set_int64(zvec_doc_t doc, const char* field, int64_t value);

int zvec_doc_set_uint32(zvec_doc_t doc, const char* field, uint32_t value);

int zvec_doc_set_uint64(zvec_doc_t doc, const char* field, uint64_t value);

int zvec_doc_set_float(zvec_doc_t doc, const char* field, float value);

int zvec_doc_set_double(zvec_doc_t doc, const char* field, double value);

int zvec_doc_set_bool(zvec_doc_t doc, const char* field, int value);

int zvec_doc_set_null(zvec_doc_t doc, const char* field);

int zvec_doc_set_vector_f32(zvec_doc_t doc, const char* field, const float* data, uint32_t dim);

int zvec_doc_set_vector_f64(zvec_doc_t doc, const char* field, const double* data, uint32_t dim);

int zvec_doc_set_sparse_vector_f32(zvec_doc_t doc, const char* field,
                                    const uint32_t* indices, const float* values, uint32_t count);

int zvec_doc_set_string_array(zvec_doc_t doc, const char* field,
                               const char** values, uint32_t count);

int zvec_doc_set_int32_array(zvec_doc_t doc, const char* field,
                              const int32_t* values, uint32_t count);

int zvec_doc_set_int64_array(zvec_doc_t doc, const char* field,
                              const int64_t* values, uint32_t count);

int zvec_doc_set_float_array(zvec_doc_t doc, const char* field,
                              const float* values, uint32_t count);

int zvec_doc_set_double_array(zvec_doc_t doc, const char* field,
                               const double* values, uint32_t count);

/* Field getters */
int zvec_doc_get_string(zvec_doc_t doc, const char* field, char** out);

int zvec_doc_get_int32(zvec_doc_t doc, const char* field, int32_t* out);

int zvec_doc_get_int64(zvec_doc_t doc, const char* field, int64_t* out);

int zvec_doc_get_uint32(zvec_doc_t doc, const char* field, uint32_t* out);

int zvec_doc_get_uint64(zvec_doc_t doc, const char* field, uint64_t* out);

int zvec_doc_get_float(zvec_doc_t doc, const char* field, float* out);

int zvec_doc_get_double(zvec_doc_t doc, const char* field, double* out);

int zvec_doc_get_bool(zvec_doc_t doc, const char* field, int* out);

int zvec_doc_has_field(zvec_doc_t doc, const char* field, int* out);

int zvec_doc_is_null(zvec_doc_t doc, const char* field, int* out);

int zvec_doc_get_vector_f32(zvec_doc_t doc, const char* field, const float** out, uint32_t* dim);

int zvec_doc_get_vector_f64(zvec_doc_t doc, const char* field, const double** out, uint32_t* dim);

int zvec_doc_get_sparse_vector_f32(zvec_doc_t doc, const char* field,
                                    const uint32_t** indices, const float** values, uint32_t* count);

int zvec_doc_get_string_array(zvec_doc_t doc, const char* field, zvec_string_array_t* out);

int zvec_doc_get_int32_array(zvec_doc_t doc, const char* field, const int32_t** out, uint32_t* count);

int zvec_doc_get_int64_array(zvec_doc_t doc, const char* field, const int64_t** out, uint32_t* count);

int zvec_doc_get_float_array(zvec_doc_t doc, const char* field, const float** out, uint32_t* count);

int zvec_doc_get_double_array(zvec_doc_t doc, const char* field, const double** out, uint32_t* count);

int zvec_doc_field_names(zvec_doc_t doc, zvec_string_array_t* out);

/* ========== DML ========== */
int zvec_collection_insert(zvec_collection_t coll, zvec_doc_t* docs, uint32_t count,
                            zvec_write_results_t* out);

int zvec_collection_upsert(zvec_collection_t coll, zvec_doc_t* docs, uint32_t count,
                            zvec_write_results_t* out);

int zvec_collection_update(zvec_collection_t coll, zvec_doc_t* docs, uint32_t count,
                            zvec_write_results_t* out);

int zvec_collection_delete(zvec_collection_t coll, const char** pks, uint32_t count,
                            zvec_write_results_t* out);

int zvec_collection_delete_by_filter(zvec_collection_t coll, const char* filter);

/* ========== DQL ========== */
int zvec_collection_query(zvec_collection_t coll, zvec_vector_query_t query,
                           zvec_doc_list_t* out);

int zvec_collection_fetch(zvec_collection_t coll, const char** pks, uint32_t count,
                           zvec_doc_map_t* out);

/* ========== VectorQuery ========== */
int zvec_vector_query_create(zvec_vector_query_t* out);

int zvec_vector_query_destroy(zvec_vector_query_t q);

int zvec_vector_query_set_field_name(zvec_vector_query_t q, const char* name);

int zvec_vector_query_set_topk(zvec_vector_query_t q, int topk);

int zvec_vector_query_set_vector_f32(zvec_vector_query_t q, const float* data, uint32_t dim);

int zvec_vector_query_set_filter(zvec_vector_query_t q, const char* filter);

int zvec_vector_query_set_include_vector(zvec_vector_query_t q, int include);

int zvec_vector_query_set_output_fields(zvec_vector_query_t q, const char** fields, uint32_t count);

int zvec_vector_query_set_query_params(zvec_vector_query_t q, zvec_query_params_t params);

int zvec_vector_query_set_sparse_vector_f32(zvec_vector_query_t q,
                                             const uint32_t* indices, const float* values,
                                             uint32_t count);

/* ========== Result containers ========== */

/* DocList */
int zvec_doc_list_count(zvec_doc_list_t list, uint32_t* out);

int zvec_doc_list_get(zvec_doc_list_t list, uint32_t index, zvec_doc_t* out);

int zvec_doc_list_destroy(zvec_doc_list_t list);

/* DocMap */
int zvec_doc_map_count(zvec_doc_map_t map, uint32_t* out);

int zvec_doc_map_keys(zvec_doc_map_t map, zvec_string_array_t* out);

int zvec_doc_map_get(zvec_doc_map_t map, const char* key, zvec_doc_t* out);

int zvec_doc_map_destroy(zvec_doc_map_t map);

/* WriteResults */
int zvec_write_results_count(zvec_write_results_t results, uint32_t* out);

int zvec_write_results_status_code(zvec_write_results_t results, uint32_t index, int* out);

int zvec_write_results_status_message(zvec_write_results_t results, uint32_t index, char** out);

int zvec_write_results_destroy(zvec_write_results_t results);

/* StringArray */
int zvec_string_array_count(zvec_string_array_t arr, uint32_t* out);

int zvec_string_array_get(zvec_string_array_t arr, uint32_t index, char** out);

int zvec_string_array_destroy(zvec_string_array_t arr);

#ifdef __cplusplus
}
#endif

#endif /* ZVEC_C_H */
