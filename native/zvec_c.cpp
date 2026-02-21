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
#include "zvec_c.h"

#include <cstring>
#include <memory>
#include <string>
#include <vector>

#include <zvec/db/collection.h>
#include <zvec/db/config.h>
#include <zvec/db/doc.h>
#include <zvec/db/index_params.h>
#include <zvec/db/options.h>
#include <zvec/db/query_params.h>
#include <zvec/db/schema.h>
#include <zvec/db/stats.h>
#include <zvec/db/status.h>
#include <zvec/db/type.h>

/* ========== Thread-local error ========== */
static thread_local std::string tl_error_message;

static void set_error(const std::string& msg) {
    tl_error_message = msg;
}

static void clear_error() {
    tl_error_message.clear();
}

static int status_to_code(const zvec::Status& s) {
    if (s.ok()) return ZVEC_OK;
    set_error(s.message());
    return static_cast<int>(s.code());
}

static int status_code_to_int(zvec::StatusCode code) {
    return static_cast<int>(code);
}

static char* copy_string(const std::string& s) {
    char* buf = static_cast<char*>(malloc(s.size() + 1));
    if (buf) {
        memcpy(buf, s.c_str(), s.size() + 1);
    }
    return buf;
}

/* ========== Wrapper structs for opaque handles ========== */

struct zvec_collection_s {
    zvec::Collection::Ptr ptr;
};

struct zvec_collection_schema_s {
    zvec::CollectionSchema::Ptr ptr;
    bool owned; // true if we should delete on destroy
};

struct zvec_field_schema_s {
    zvec::FieldSchema::Ptr ptr;
    bool owned;
};

struct zvec_index_params_s {
    zvec::IndexParams::Ptr ptr;
};

struct zvec_query_params_s {
    zvec::QueryParams::Ptr ptr;
};

struct zvec_doc_s {
    zvec::Doc::Ptr ptr;
    bool owned;
};

struct zvec_doc_list_s {
    zvec::DocPtrList list;
};

struct zvec_doc_map_s {
    zvec::DocPtrMap map;
};

struct zvec_write_results_s {
    zvec::WriteResults results;
};

struct zvec_string_array_s {
    std::vector<std::string> strings;
};

struct zvec_vector_query_s {
    zvec::VectorQuery query;
};

/* ========== Error message ========== */

extern "C" const char* zvec_last_error_message(void) {
    return tl_error_message.c_str();
}

extern "C" void zvec_free_string(char* str) {
    free(str);
}

/* ========== Global config ========== */

extern "C" int zvec_global_config_init(uint32_t log_level, uint32_t query_threads, uint32_t optimize_threads) {
    try {
        clear_error();
        zvec::GlobalConfig::ConfigData data;
        data.log_config = std::make_shared<zvec::GlobalConfig::ConsoleLogConfig>(
            static_cast<zvec::GlobalConfig::LogLevel>(log_level));
        data.query_thread_count = query_threads;
        data.optimize_thread_count = optimize_threads;
        auto status = zvec::GlobalConfig::Instance().Initialize(data);
        return status_to_code(status);
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== Collection lifecycle ========== */

extern "C" int zvec_collection_create_and_open(const char* path,
                                                zvec_collection_schema_t schema,
                                                int read_only, int enable_mmap, uint32_t max_buffer_size,
                                                zvec_collection_t* out) {
    try {
        clear_error();
        zvec::CollectionOptions opts(read_only != 0, enable_mmap != 0, max_buffer_size);
        auto result = zvec::Collection::CreateAndOpen(path, *schema->ptr, opts);
        if (!result) {
            return status_to_code(result.error());
        }
        auto* wrapper = new zvec_collection_s{std::move(result.value())};
        *out = wrapper;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_open(const char* path,
                                     int read_only, int enable_mmap, uint32_t max_buffer_size,
                                     zvec_collection_t* out) {
    try {
        clear_error();
        zvec::CollectionOptions opts(read_only != 0, enable_mmap != 0, max_buffer_size);
        auto result = zvec::Collection::Open(path, opts);
        if (!result) {
            return status_to_code(result.error());
        }
        auto* wrapper = new zvec_collection_s{std::move(result.value())};
        *out = wrapper;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_close(zvec_collection_t coll) {
    try {
        clear_error();
        delete coll;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_destroy(zvec_collection_t coll) {
    try {
        clear_error();
        auto status = coll->ptr->Destroy();
        delete coll;
        return status_to_code(status);
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_flush(zvec_collection_t coll) {
    try {
        clear_error();
        return status_to_code(coll->ptr->Flush());
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== Collection properties ========== */

extern "C" int zvec_collection_path(zvec_collection_t coll, char** out) {
    try {
        clear_error();
        auto result = coll->ptr->Path();
        if (!result) {
            return status_to_code(result.error());
        }
        *out = copy_string(result.value());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_stats(zvec_collection_t coll, uint64_t* doc_count) {
    try {
        clear_error();
        auto result = coll->ptr->Stats();
        if (!result) {
            return status_to_code(result.error());
        }
        *doc_count = result.value().doc_count;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_schema(zvec_collection_t coll, zvec_collection_schema_t* out) {
    try {
        clear_error();
        auto result = coll->ptr->Schema();
        if (!result) {
            return status_to_code(result.error());
        }
        auto schema_ptr = std::make_shared<zvec::CollectionSchema>(result.value());
        auto* wrapper = new zvec_collection_schema_s{schema_ptr, true};
        *out = wrapper;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== CollectionSchema ========== */

extern "C" int zvec_collection_schema_create(const char* name, zvec_collection_schema_t* out) {
    try {
        clear_error();
        auto ptr = std::make_shared<zvec::CollectionSchema>(name);
        *out = new zvec_collection_schema_s{ptr, true};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_schema_add_field(zvec_collection_schema_t schema, zvec_field_schema_t field) {
    try {
        clear_error();
        auto status = schema->ptr->add_field(field->ptr);
        return status_to_code(status);
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_schema_destroy(zvec_collection_schema_t schema) {
    try {
        clear_error();
        if (schema && schema->owned) {
            delete schema;
        }
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_schema_name(zvec_collection_schema_t schema, char** out) {
    try {
        clear_error();
        *out = copy_string(schema->ptr->name());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_schema_field_count(zvec_collection_schema_t schema, uint32_t* out) {
    try {
        clear_error();
        *out = static_cast<uint32_t>(schema->ptr->fields().size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_schema_field_at(zvec_collection_schema_t schema, uint32_t index, zvec_field_schema_t* out) {
    try {
        clear_error();
        auto fields = schema->ptr->fields();
        if (index >= fields.size()) {
            set_error("Index out of bounds");
            return ZVEC_INVALID_ARGUMENT;
        }
        *out = new zvec_field_schema_s{fields[index], false};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_schema_field_by_name(zvec_collection_schema_t schema, const char* name, zvec_field_schema_t* out) {
    try {
        clear_error();
        auto* field = schema->ptr->get_field(name);
        if (!field) {
            set_error(std::string("Field not found: ") + name);
            return ZVEC_NOT_FOUND;
        }
        auto field_ptr = std::make_shared<zvec::FieldSchema>(*field);
        *out = new zvec_field_schema_s{field_ptr, false};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== FieldSchema ========== */

extern "C" int zvec_field_schema_create(const char* name, uint32_t data_type, int nullable,
                                         zvec_index_params_t index_params,
                                         zvec_field_schema_t* out) {
    try {
        clear_error();
        zvec::IndexParams::Ptr ip = index_params ? index_params->ptr : nullptr;
        auto ptr = std::make_shared<zvec::FieldSchema>(
            name, static_cast<zvec::DataType>(data_type), nullable != 0, ip);
        *out = new zvec_field_schema_s{ptr, true};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_field_schema_create_vector(const char* name, uint32_t data_type, uint32_t dimension,
                                                int nullable, zvec_index_params_t index_params,
                                                zvec_field_schema_t* out) {
    try {
        clear_error();
        zvec::IndexParams::Ptr ip = index_params ? index_params->ptr : nullptr;
        auto ptr = std::make_shared<zvec::FieldSchema>(
            name, static_cast<zvec::DataType>(data_type), dimension, nullable != 0, ip);
        *out = new zvec_field_schema_s{ptr, true};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_field_schema_destroy(zvec_field_schema_t fs) {
    try {
        clear_error();
        delete fs;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_field_schema_name(zvec_field_schema_t fs, char** out) {
    try {
        clear_error();
        *out = copy_string(fs->ptr->name());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_field_schema_data_type(zvec_field_schema_t fs, uint32_t* out) {
    try {
        clear_error();
        *out = static_cast<uint32_t>(fs->ptr->data_type());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_field_schema_dimension(zvec_field_schema_t fs, uint32_t* out) {
    try {
        clear_error();
        *out = fs->ptr->dimension();
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_field_schema_nullable(zvec_field_schema_t fs, int* out) {
    try {
        clear_error();
        *out = fs->ptr->nullable() ? 1 : 0;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_field_schema_index_type(zvec_field_schema_t fs, uint32_t* out) {
    try {
        clear_error();
        *out = static_cast<uint32_t>(fs->ptr->index_type());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== Index params factories ========== */

extern "C" int zvec_hnsw_index_params_create(uint32_t metric_type, int m, int ef_construction,
                                              uint32_t quantize_type, zvec_index_params_t* out) {
    try {
        clear_error();
        auto ptr = std::make_shared<zvec::HnswIndexParams>(
            static_cast<zvec::MetricType>(metric_type), m, ef_construction,
            static_cast<zvec::QuantizeType>(quantize_type));
        *out = new zvec_index_params_s{ptr};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_flat_index_params_create(uint32_t metric_type, uint32_t quantize_type,
                                              zvec_index_params_t* out) {
    try {
        clear_error();
        auto ptr = std::make_shared<zvec::FlatIndexParams>(
            static_cast<zvec::MetricType>(metric_type),
            static_cast<zvec::QuantizeType>(quantize_type));
        *out = new zvec_index_params_s{ptr};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_ivf_index_params_create(uint32_t metric_type, int n_list, int n_iters,
                                             int use_soar, uint32_t quantize_type,
                                             zvec_index_params_t* out) {
    try {
        clear_error();
        auto ptr = std::make_shared<zvec::IVFIndexParams>(
            static_cast<zvec::MetricType>(metric_type), n_list, n_iters,
            use_soar != 0, static_cast<zvec::QuantizeType>(quantize_type));
        *out = new zvec_index_params_s{ptr};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_invert_index_params_create(int enable_range_optimization,
                                                int enable_extended_wildcard,
                                                zvec_index_params_t* out) {
    try {
        clear_error();
        auto ptr = std::make_shared<zvec::InvertIndexParams>(
            enable_range_optimization != 0, enable_extended_wildcard != 0);
        *out = new zvec_index_params_s{ptr};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_index_params_destroy(zvec_index_params_t params) {
    try {
        clear_error();
        delete params;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== Query params factories ========== */

extern "C" int zvec_hnsw_query_params_create(int ef, float radius, int is_linear,
                                              int is_using_refiner, zvec_query_params_t* out) {
    try {
        clear_error();
        auto ptr = std::make_shared<zvec::HnswQueryParams>(ef, radius, is_linear != 0, is_using_refiner != 0);
        *out = new zvec_query_params_s{ptr};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_flat_query_params_create(int is_using_refiner, float scale_factor,
                                              zvec_query_params_t* out) {
    try {
        clear_error();
        auto ptr = std::make_shared<zvec::FlatQueryParams>(is_using_refiner != 0, scale_factor);
        *out = new zvec_query_params_s{ptr};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_ivf_query_params_create(int nprobe, int is_using_refiner, float scale_factor,
                                             zvec_query_params_t* out) {
    try {
        clear_error();
        auto ptr = std::make_shared<zvec::IVFQueryParams>(nprobe, is_using_refiner != 0, scale_factor);
        *out = new zvec_query_params_s{ptr};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_query_params_destroy(zvec_query_params_t params) {
    try {
        clear_error();
        delete params;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== Index DDL ========== */

extern "C" int zvec_collection_create_index(zvec_collection_t coll, const char* field_name,
                                             zvec_index_params_t index_params, int concurrency) {
    try {
        clear_error();
        zvec::CreateIndexOptions opts{concurrency};
        return status_to_code(coll->ptr->CreateIndex(field_name, index_params->ptr, opts));
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_drop_index(zvec_collection_t coll, const char* field_name) {
    try {
        clear_error();
        return status_to_code(coll->ptr->DropIndex(field_name));
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_optimize(zvec_collection_t coll, int concurrency) {
    try {
        clear_error();
        zvec::OptimizeOptions opts{concurrency};
        return status_to_code(coll->ptr->Optimize(opts));
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== Column DDL ========== */

extern "C" int zvec_collection_add_column(zvec_collection_t coll, zvec_field_schema_t field_schema,
                                           const char* expression, int concurrency) {
    try {
        clear_error();
        zvec::AddColumnOptions opts{concurrency};
        return status_to_code(coll->ptr->AddColumn(field_schema->ptr, expression ? expression : "", opts));
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_drop_column(zvec_collection_t coll, const char* field_name) {
    try {
        clear_error();
        return status_to_code(coll->ptr->DropColumn(field_name));
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_alter_column(zvec_collection_t coll, const char* old_name,
                                             const char* new_name, zvec_field_schema_t new_schema,
                                             int concurrency) {
    try {
        clear_error();
        zvec::AlterColumnOptions opts{concurrency};
        zvec::FieldSchema::Ptr schema_ptr = new_schema ? new_schema->ptr : nullptr;
        return status_to_code(coll->ptr->AlterColumn(old_name, new_name ? new_name : "", schema_ptr, opts));
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== Doc ========== */

extern "C" int zvec_doc_create(zvec_doc_t* out) {
    try {
        clear_error();
        auto ptr = std::make_shared<zvec::Doc>();
        *out = new zvec_doc_s{ptr, true};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_destroy(zvec_doc_t doc) {
    try {
        clear_error();
        delete doc;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_pk(zvec_doc_t doc, const char* pk) {
    try {
        clear_error();
        doc->ptr->set_pk(pk);
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_pk(zvec_doc_t doc, char** out) {
    try {
        clear_error();
        *out = copy_string(doc->ptr->pk());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_score(zvec_doc_t doc, float* out) {
    try {
        clear_error();
        *out = doc->ptr->score();
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* Field setters */

extern "C" int zvec_doc_set_string(zvec_doc_t doc, const char* field, const char* value) {
    try {
        clear_error();
        doc->ptr->set(field, std::string(value));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_int32(zvec_doc_t doc, const char* field, int32_t value) {
    try {
        clear_error();
        doc->ptr->set(field, value);
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_int64(zvec_doc_t doc, const char* field, int64_t value) {
    try {
        clear_error();
        doc->ptr->set(field, value);
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_uint32(zvec_doc_t doc, const char* field, uint32_t value) {
    try {
        clear_error();
        doc->ptr->set(field, value);
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_uint64(zvec_doc_t doc, const char* field, uint64_t value) {
    try {
        clear_error();
        doc->ptr->set(field, value);
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_float(zvec_doc_t doc, const char* field, float value) {
    try {
        clear_error();
        doc->ptr->set(field, value);
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_double(zvec_doc_t doc, const char* field, double value) {
    try {
        clear_error();
        doc->ptr->set(field, value);
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_bool(zvec_doc_t doc, const char* field, int value) {
    try {
        clear_error();
        doc->ptr->set(field, static_cast<bool>(value));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_null(zvec_doc_t doc, const char* field) {
    try {
        clear_error();
        doc->ptr->set_null(field);
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_vector_f32(zvec_doc_t doc, const char* field, const float* data, uint32_t dim) {
    try {
        clear_error();
        std::vector<float> vec(data, data + dim);
        doc->ptr->set(field, std::move(vec));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_vector_f64(zvec_doc_t doc, const char* field, const double* data, uint32_t dim) {
    try {
        clear_error();
        std::vector<double> vec(data, data + dim);
        doc->ptr->set(field, std::move(vec));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_sparse_vector_f32(zvec_doc_t doc, const char* field,
                                               const uint32_t* indices, const float* values, uint32_t count) {
    try {
        clear_error();
        std::vector<uint32_t> idx(indices, indices + count);
        std::vector<float> vals(values, values + count);
        doc->ptr->set(field, std::make_pair(std::move(idx), std::move(vals)));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_string_array(zvec_doc_t doc, const char* field,
                                          const char** values, uint32_t count) {
    try {
        clear_error();
        std::vector<std::string> arr;
        arr.reserve(count);
        for (uint32_t i = 0; i < count; i++) {
            arr.emplace_back(values[i]);
        }
        doc->ptr->set(field, std::move(arr));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_int32_array(zvec_doc_t doc, const char* field,
                                         const int32_t* values, uint32_t count) {
    try {
        clear_error();
        std::vector<int32_t> arr(values, values + count);
        doc->ptr->set(field, std::move(arr));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_int64_array(zvec_doc_t doc, const char* field,
                                         const int64_t* values, uint32_t count) {
    try {
        clear_error();
        std::vector<int64_t> arr(values, values + count);
        doc->ptr->set(field, std::move(arr));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_float_array(zvec_doc_t doc, const char* field,
                                         const float* values, uint32_t count) {
    try {
        clear_error();
        std::vector<float> arr(values, values + count);
        doc->ptr->set(field, std::move(arr));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_set_double_array(zvec_doc_t doc, const char* field,
                                          const double* values, uint32_t count) {
    try {
        clear_error();
        std::vector<double> arr(values, values + count);
        doc->ptr->set(field, std::move(arr));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* Field getters */

extern "C" int zvec_doc_get_string(zvec_doc_t doc, const char* field, char** out) {
    try {
        clear_error();
        auto result = doc->ptr->get<std::string>(field);
        if (!result) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        *out = copy_string(result.value());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_int32(zvec_doc_t doc, const char* field, int32_t* out) {
    try {
        clear_error();
        auto result = doc->ptr->get<int32_t>(field);
        if (!result) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        *out = result.value();
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_int64(zvec_doc_t doc, const char* field, int64_t* out) {
    try {
        clear_error();
        auto result = doc->ptr->get<int64_t>(field);
        if (!result) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        *out = result.value();
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_uint32(zvec_doc_t doc, const char* field, uint32_t* out) {
    try {
        clear_error();
        auto result = doc->ptr->get<uint32_t>(field);
        if (!result) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        *out = result.value();
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_uint64(zvec_doc_t doc, const char* field, uint64_t* out) {
    try {
        clear_error();
        auto result = doc->ptr->get<uint64_t>(field);
        if (!result) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        *out = result.value();
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_float(zvec_doc_t doc, const char* field, float* out) {
    try {
        clear_error();
        auto result = doc->ptr->get<float>(field);
        if (!result) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        *out = result.value();
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_double(zvec_doc_t doc, const char* field, double* out) {
    try {
        clear_error();
        auto result = doc->ptr->get<double>(field);
        if (!result) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        *out = result.value();
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_bool(zvec_doc_t doc, const char* field, int* out) {
    try {
        clear_error();
        auto result = doc->ptr->get<bool>(field);
        if (!result) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        *out = result.value() ? 1 : 0;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_has_field(zvec_doc_t doc, const char* field, int* out) {
    try {
        clear_error();
        *out = doc->ptr->has(field) ? 1 : 0;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_is_null(zvec_doc_t doc, const char* field, int* out) {
    try {
        clear_error();
        *out = doc->ptr->is_null(field) ? 1 : 0;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_vector_f32(zvec_doc_t doc, const char* field, const float** out, uint32_t* dim) {
    try {
        clear_error();
        auto result = doc->ptr->get_field<std::vector<float>>(field);
        if (!result.ok()) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        auto& vec = result.value();
        *out = vec.data();
        *dim = static_cast<uint32_t>(vec.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_vector_f64(zvec_doc_t doc, const char* field, const double** out, uint32_t* dim) {
    try {
        clear_error();
        auto result = doc->ptr->get_field<std::vector<double>>(field);
        if (!result.ok()) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        auto& vec = result.value();
        *out = vec.data();
        *dim = static_cast<uint32_t>(vec.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_sparse_vector_f32(zvec_doc_t doc, const char* field,
                                               const uint32_t** indices, const float** values, uint32_t* count) {
    try {
        clear_error();
        using SparseVec = std::pair<std::vector<uint32_t>, std::vector<float>>;
        auto result = doc->ptr->get_field<SparseVec>(field);
        if (!result.ok()) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        auto& sv = result.value();
        *indices = sv.first.data();
        *values = sv.second.data();
        *count = static_cast<uint32_t>(sv.first.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_string_array(zvec_doc_t doc, const char* field, zvec_string_array_t* out) {
    try {
        clear_error();
        auto result = doc->ptr->get<std::vector<std::string>>(field);
        if (!result) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        *out = new zvec_string_array_s{result.value()};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_int32_array(zvec_doc_t doc, const char* field, const int32_t** out, uint32_t* count) {
    try {
        clear_error();
        auto result = doc->ptr->get_field<std::vector<int32_t>>(field);
        if (!result.ok()) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        auto& vec = result.value();
        *out = vec.data();
        *count = static_cast<uint32_t>(vec.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_int64_array(zvec_doc_t doc, const char* field, const int64_t** out, uint32_t* count) {
    try {
        clear_error();
        auto result = doc->ptr->get_field<std::vector<int64_t>>(field);
        if (!result.ok()) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        auto& vec = result.value();
        *out = vec.data();
        *count = static_cast<uint32_t>(vec.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_float_array(zvec_doc_t doc, const char* field, const float** out, uint32_t* count) {
    try {
        clear_error();
        auto result = doc->ptr->get_field<std::vector<float>>(field);
        if (!result.ok()) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        auto& vec = result.value();
        *out = vec.data();
        *count = static_cast<uint32_t>(vec.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_get_double_array(zvec_doc_t doc, const char* field, const double** out, uint32_t* count) {
    try {
        clear_error();
        auto result = doc->ptr->get_field<std::vector<double>>(field);
        if (!result.ok()) {
            set_error(std::string("Field not found or type mismatch: ") + field);
            return ZVEC_NOT_FOUND;
        }
        auto& vec = result.value();
        *out = vec.data();
        *count = static_cast<uint32_t>(vec.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_field_names(zvec_doc_t doc, zvec_string_array_t* out) {
    try {
        clear_error();
        *out = new zvec_string_array_s{doc->ptr->field_names()};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== DML ========== */

static std::vector<zvec::Doc> docs_from_handles(zvec_doc_t* docs, uint32_t count) {
    std::vector<zvec::Doc> result;
    result.reserve(count);
    for (uint32_t i = 0; i < count; i++) {
        result.push_back(*docs[i]->ptr);
    }
    return result;
}

extern "C" int zvec_collection_insert(zvec_collection_t coll, zvec_doc_t* docs, uint32_t count,
                                       zvec_write_results_t* out) {
    try {
        clear_error();
        auto doc_vec = docs_from_handles(docs, count);
        auto result = coll->ptr->Insert(doc_vec);
        if (!result) {
            return status_to_code(result.error());
        }
        *out = new zvec_write_results_s{std::move(result.value())};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_upsert(zvec_collection_t coll, zvec_doc_t* docs, uint32_t count,
                                       zvec_write_results_t* out) {
    try {
        clear_error();
        auto doc_vec = docs_from_handles(docs, count);
        auto result = coll->ptr->Upsert(doc_vec);
        if (!result) {
            return status_to_code(result.error());
        }
        *out = new zvec_write_results_s{std::move(result.value())};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_update(zvec_collection_t coll, zvec_doc_t* docs, uint32_t count,
                                       zvec_write_results_t* out) {
    try {
        clear_error();
        auto doc_vec = docs_from_handles(docs, count);
        auto result = coll->ptr->Update(doc_vec);
        if (!result) {
            return status_to_code(result.error());
        }
        *out = new zvec_write_results_s{std::move(result.value())};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_delete(zvec_collection_t coll, const char** pks, uint32_t count,
                                       zvec_write_results_t* out) {
    try {
        clear_error();
        std::vector<std::string> pk_vec;
        pk_vec.reserve(count);
        for (uint32_t i = 0; i < count; i++) {
            pk_vec.emplace_back(pks[i]);
        }
        auto result = coll->ptr->Delete(pk_vec);
        if (!result) {
            return status_to_code(result.error());
        }
        *out = new zvec_write_results_s{std::move(result.value())};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_delete_by_filter(zvec_collection_t coll, const char* filter) {
    try {
        clear_error();
        return status_to_code(coll->ptr->DeleteByFilter(filter));
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== DQL ========== */

extern "C" int zvec_collection_query(zvec_collection_t coll, zvec_vector_query_t query,
                                      zvec_doc_list_t* out) {
    try {
        clear_error();
        auto result = coll->ptr->Query(query->query);
        if (!result) {
            return status_to_code(result.error());
        }
        *out = new zvec_doc_list_s{std::move(result.value())};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_collection_fetch(zvec_collection_t coll, const char** pks, uint32_t count,
                                      zvec_doc_map_t* out) {
    try {
        clear_error();
        std::vector<std::string> pk_vec;
        pk_vec.reserve(count);
        for (uint32_t i = 0; i < count; i++) {
            pk_vec.emplace_back(pks[i]);
        }
        auto result = coll->ptr->Fetch(pk_vec);
        if (!result) {
            return status_to_code(result.error());
        }
        *out = new zvec_doc_map_s{std::move(result.value())};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== VectorQuery ========== */

extern "C" int zvec_vector_query_create(zvec_vector_query_t* out) {
    try {
        clear_error();
        *out = new zvec_vector_query_s{};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_vector_query_destroy(zvec_vector_query_t q) {
    try {
        clear_error();
        delete q;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_vector_query_set_field_name(zvec_vector_query_t q, const char* name) {
    try {
        clear_error();
        q->query.field_name_ = name;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_vector_query_set_topk(zvec_vector_query_t q, int topk) {
    try {
        clear_error();
        q->query.topk_ = topk;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_vector_query_set_vector_f32(zvec_vector_query_t q, const float* data, uint32_t dim) {
    try {
        clear_error();
        // Store the raw bytes of the float array as a string (binary blob)
        q->query.query_vector_ = std::string(reinterpret_cast<const char*>(data), dim * sizeof(float));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_vector_query_set_filter(zvec_vector_query_t q, const char* filter) {
    try {
        clear_error();
        q->query.filter_ = filter ? filter : "";
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_vector_query_set_include_vector(zvec_vector_query_t q, int include) {
    try {
        clear_error();
        q->query.include_vector_ = include != 0;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_vector_query_set_output_fields(zvec_vector_query_t q, const char** fields, uint32_t count) {
    try {
        clear_error();
        std::vector<std::string> field_vec;
        field_vec.reserve(count);
        for (uint32_t i = 0; i < count; i++) {
            field_vec.emplace_back(fields[i]);
        }
        q->query.output_fields_ = std::move(field_vec);
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_vector_query_set_query_params(zvec_vector_query_t q, zvec_query_params_t params) {
    try {
        clear_error();
        q->query.query_params_ = params ? params->ptr : nullptr;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_vector_query_set_sparse_vector_f32(zvec_vector_query_t q,
                                                        const uint32_t* indices, const float* values,
                                                        uint32_t count) {
    try {
        clear_error();
        q->query.query_sparse_indices_ = std::string(
            reinterpret_cast<const char*>(indices), count * sizeof(uint32_t));
        q->query.query_sparse_values_ = std::string(
            reinterpret_cast<const char*>(values), count * sizeof(float));
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* ========== Result containers ========== */

/* DocList */
extern "C" int zvec_doc_list_count(zvec_doc_list_t list, uint32_t* out) {
    try {
        clear_error();
        *out = static_cast<uint32_t>(list->list.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_list_get(zvec_doc_list_t list, uint32_t index, zvec_doc_t* out) {
    try {
        clear_error();
        if (index >= list->list.size()) {
            set_error("Index out of bounds");
            return ZVEC_INVALID_ARGUMENT;
        }
        *out = new zvec_doc_s{list->list[index], false};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_list_destroy(zvec_doc_list_t list) {
    try {
        clear_error();
        delete list;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* DocMap */
extern "C" int zvec_doc_map_count(zvec_doc_map_t map, uint32_t* out) {
    try {
        clear_error();
        *out = static_cast<uint32_t>(map->map.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_map_keys(zvec_doc_map_t map, zvec_string_array_t* out) {
    try {
        clear_error();
        std::vector<std::string> keys;
        keys.reserve(map->map.size());
        for (const auto& [k, v] : map->map) {
            if (v) keys.push_back(k);  // skip null doc entries (missing keys)
        }
        *out = new zvec_string_array_s{std::move(keys)};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_map_get(zvec_doc_map_t map, const char* key, zvec_doc_t* out) {
    try {
        clear_error();
        auto it = map->map.find(key);
        if (it == map->map.end()) {
            set_error(std::string("Key not found: ") + key);
            return ZVEC_NOT_FOUND;
        }
        *out = new zvec_doc_s{it->second, false};
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_doc_map_destroy(zvec_doc_map_t map) {
    try {
        clear_error();
        delete map;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* WriteResults */
extern "C" int zvec_write_results_count(zvec_write_results_t results, uint32_t* out) {
    try {
        clear_error();
        *out = static_cast<uint32_t>(results->results.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_write_results_status_code(zvec_write_results_t results, uint32_t index, int* out) {
    try {
        clear_error();
        if (index >= results->results.size()) {
            set_error("Index out of bounds");
            return ZVEC_INVALID_ARGUMENT;
        }
        *out = static_cast<int>(results->results[index].code());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_write_results_status_message(zvec_write_results_t results, uint32_t index, char** out) {
    try {
        clear_error();
        if (index >= results->results.size()) {
            set_error("Index out of bounds");
            return ZVEC_INVALID_ARGUMENT;
        }
        *out = copy_string(results->results[index].message());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_write_results_destroy(zvec_write_results_t results) {
    try {
        clear_error();
        delete results;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

/* StringArray */
extern "C" int zvec_string_array_count(zvec_string_array_t arr, uint32_t* out) {
    try {
        clear_error();
        *out = static_cast<uint32_t>(arr->strings.size());
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_string_array_get(zvec_string_array_t arr, uint32_t index, char** out) {
    try {
        clear_error();
        if (index >= arr->strings.size()) {
            set_error("Index out of bounds");
            return ZVEC_INVALID_ARGUMENT;
        }
        *out = copy_string(arr->strings[index]);
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}

extern "C" int zvec_string_array_destroy(zvec_string_array_t arr) {
    try {
        clear_error();
        delete arr;
        return ZVEC_OK;
    } catch (const std::exception& e) {
        set_error(e.what());
        return ZVEC_INTERNAL_ERROR;
    }
}
