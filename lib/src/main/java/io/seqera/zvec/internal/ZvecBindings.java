package io.seqera.zvec.internal;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

public final class ZvecBindings {
    private ZvecBindings() {}

    private static final Linker LINKER = Linker.nativeLinker();

    private static MethodHandle downcall(String name, FunctionDescriptor desc) {
        var sym = NativeLoader.symbolLookup().find(name)
                .orElseThrow(() -> new UnsatisfiedLinkError("Missing native symbol: " + name));
        return LINKER.downcallHandle(sym, desc);
    }

    // Shorthand layouts
    private static final ValueLayout.OfInt C_INT = JAVA_INT;
    private static final ValueLayout.OfFloat C_FLOAT = JAVA_FLOAT;
    private static final ValueLayout.OfDouble C_DOUBLE = JAVA_DOUBLE;
    private static final ValueLayout.OfLong C_LONG = JAVA_LONG;

    // ========== Error message ==========
    static final MethodHandle zvec_last_error_message = downcall("zvec_last_error_message",
            FunctionDescriptor.of(ADDRESS));

    static final MethodHandle zvec_free_string = downcall("zvec_free_string",
            FunctionDescriptor.ofVoid(ADDRESS));

    // ========== Global config ==========
    public static final MethodHandle zvec_global_config_init = downcall("zvec_global_config_init",
            FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT));

    // ========== Collection lifecycle ==========
    public static final MethodHandle zvec_collection_create_and_open = downcall("zvec_collection_create_and_open",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT, C_INT, C_INT, ADDRESS));

    public static final MethodHandle zvec_collection_open = downcall("zvec_collection_open",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT, C_INT, C_INT, ADDRESS));

    public static final MethodHandle zvec_collection_close = downcall("zvec_collection_close",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_collection_destroy = downcall("zvec_collection_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_collection_flush = downcall("zvec_collection_flush",
            FunctionDescriptor.of(C_INT, ADDRESS));

    // ========== Collection properties ==========
    public static final MethodHandle zvec_collection_path = downcall("zvec_collection_path",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_collection_stats = downcall("zvec_collection_stats",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_collection_schema = downcall("zvec_collection_schema",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    // ========== CollectionSchema ==========
    public static final MethodHandle zvec_collection_schema_create = downcall("zvec_collection_schema_create",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_collection_schema_add_field = downcall("zvec_collection_schema_add_field",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_collection_schema_destroy = downcall("zvec_collection_schema_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_collection_schema_name = downcall("zvec_collection_schema_name",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_collection_schema_field_count = downcall("zvec_collection_schema_field_count",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_collection_schema_field_at = downcall("zvec_collection_schema_field_at",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT, ADDRESS));

    public static final MethodHandle zvec_collection_schema_field_by_name = downcall("zvec_collection_schema_field_by_name",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    // ========== FieldSchema ==========
    public static final MethodHandle zvec_field_schema_create = downcall("zvec_field_schema_create",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT, C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_field_schema_create_vector = downcall("zvec_field_schema_create_vector",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT, C_INT, C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_field_schema_destroy = downcall("zvec_field_schema_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_field_schema_name = downcall("zvec_field_schema_name",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_field_schema_data_type = downcall("zvec_field_schema_data_type",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_field_schema_dimension = downcall("zvec_field_schema_dimension",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_field_schema_nullable = downcall("zvec_field_schema_nullable",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_field_schema_index_type = downcall("zvec_field_schema_index_type",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    // ========== Index params ==========
    public static final MethodHandle zvec_hnsw_index_params_create = downcall("zvec_hnsw_index_params_create",
            FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT, C_INT, ADDRESS));

    public static final MethodHandle zvec_flat_index_params_create = downcall("zvec_flat_index_params_create",
            FunctionDescriptor.of(C_INT, C_INT, C_INT, ADDRESS));

    public static final MethodHandle zvec_ivf_index_params_create = downcall("zvec_ivf_index_params_create",
            FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT, C_INT, C_INT, ADDRESS));

    public static final MethodHandle zvec_invert_index_params_create = downcall("zvec_invert_index_params_create",
            FunctionDescriptor.of(C_INT, C_INT, C_INT, ADDRESS));

    public static final MethodHandle zvec_index_params_destroy = downcall("zvec_index_params_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));

    // ========== Query params ==========
    public static final MethodHandle zvec_hnsw_query_params_create = downcall("zvec_hnsw_query_params_create",
            FunctionDescriptor.of(C_INT, C_INT, C_FLOAT, C_INT, C_INT, ADDRESS));

    public static final MethodHandle zvec_flat_query_params_create = downcall("zvec_flat_query_params_create",
            FunctionDescriptor.of(C_INT, C_INT, C_FLOAT, ADDRESS));

    public static final MethodHandle zvec_ivf_query_params_create = downcall("zvec_ivf_query_params_create",
            FunctionDescriptor.of(C_INT, C_INT, C_INT, C_FLOAT, ADDRESS));

    public static final MethodHandle zvec_query_params_destroy = downcall("zvec_query_params_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));

    // ========== Index DDL ==========
    public static final MethodHandle zvec_collection_create_index = downcall("zvec_collection_create_index",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_collection_drop_index = downcall("zvec_collection_drop_index",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_collection_optimize = downcall("zvec_collection_optimize",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT));

    // ========== Column DDL ==========
    public static final MethodHandle zvec_collection_add_column = downcall("zvec_collection_add_column",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_collection_drop_column = downcall("zvec_collection_drop_column",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_collection_alter_column = downcall("zvec_collection_alter_column",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, C_INT));

    // ========== Doc ==========
    public static final MethodHandle zvec_doc_create = downcall("zvec_doc_create",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_doc_destroy = downcall("zvec_doc_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_doc_set_pk = downcall("zvec_doc_set_pk",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_pk = downcall("zvec_doc_get_pk",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_score = downcall("zvec_doc_get_score",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    // Doc field setters
    public static final MethodHandle zvec_doc_set_string = downcall("zvec_doc_set_string",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_set_int32 = downcall("zvec_doc_set_int32",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_doc_set_int64 = downcall("zvec_doc_set_int64",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_LONG));

    public static final MethodHandle zvec_doc_set_uint32 = downcall("zvec_doc_set_uint32",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_doc_set_uint64 = downcall("zvec_doc_set_uint64",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_LONG));

    public static final MethodHandle zvec_doc_set_float = downcall("zvec_doc_set_float",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_FLOAT));

    public static final MethodHandle zvec_doc_set_double = downcall("zvec_doc_set_double",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_DOUBLE));

    public static final MethodHandle zvec_doc_set_bool = downcall("zvec_doc_set_bool",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_doc_set_null = downcall("zvec_doc_set_null",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_set_vector_f32 = downcall("zvec_doc_set_vector_f32",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_doc_set_vector_f64 = downcall("zvec_doc_set_vector_f64",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_doc_set_sparse_vector_f32 = downcall("zvec_doc_set_sparse_vector_f32",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_doc_set_string_array = downcall("zvec_doc_set_string_array",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_doc_set_int32_array = downcall("zvec_doc_set_int32_array",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_doc_set_int64_array = downcall("zvec_doc_set_int64_array",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_doc_set_float_array = downcall("zvec_doc_set_float_array",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_doc_set_double_array = downcall("zvec_doc_set_double_array",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT));

    // Doc field getters
    public static final MethodHandle zvec_doc_get_string = downcall("zvec_doc_get_string",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_int32 = downcall("zvec_doc_get_int32",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_int64 = downcall("zvec_doc_get_int64",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_uint32 = downcall("zvec_doc_get_uint32",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_uint64 = downcall("zvec_doc_get_uint64",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_float = downcall("zvec_doc_get_float",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_double = downcall("zvec_doc_get_double",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_bool = downcall("zvec_doc_get_bool",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_has_field = downcall("zvec_doc_has_field",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_is_null = downcall("zvec_doc_is_null",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_vector_f32 = downcall("zvec_doc_get_vector_f32",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_vector_f64 = downcall("zvec_doc_get_vector_f64",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_sparse_vector_f32 = downcall("zvec_doc_get_sparse_vector_f32",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_string_array = downcall("zvec_doc_get_string_array",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_int32_array = downcall("zvec_doc_get_int32_array",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_int64_array = downcall("zvec_doc_get_int64_array",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_float_array = downcall("zvec_doc_get_float_array",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_get_double_array = downcall("zvec_doc_get_double_array",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_field_names = downcall("zvec_doc_field_names",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    // ========== DML ==========
    public static final MethodHandle zvec_collection_insert = downcall("zvec_collection_insert",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT, ADDRESS));

    public static final MethodHandle zvec_collection_upsert = downcall("zvec_collection_upsert",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT, ADDRESS));

    public static final MethodHandle zvec_collection_update = downcall("zvec_collection_update",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT, ADDRESS));

    public static final MethodHandle zvec_collection_delete = downcall("zvec_collection_delete",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT, ADDRESS));

    public static final MethodHandle zvec_collection_delete_by_filter = downcall("zvec_collection_delete_by_filter",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    // ========== DQL ==========
    public static final MethodHandle zvec_collection_query = downcall("zvec_collection_query",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_collection_fetch = downcall("zvec_collection_fetch",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT, ADDRESS));

    // ========== VectorQuery ==========
    public static final MethodHandle zvec_vector_query_create = downcall("zvec_vector_query_create",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_vector_query_destroy = downcall("zvec_vector_query_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_vector_query_set_field_name = downcall("zvec_vector_query_set_field_name",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_vector_query_set_topk = downcall("zvec_vector_query_set_topk",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT));

    public static final MethodHandle zvec_vector_query_set_vector_f32 = downcall("zvec_vector_query_set_vector_f32",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_vector_query_set_filter = downcall("zvec_vector_query_set_filter",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_vector_query_set_include_vector = downcall("zvec_vector_query_set_include_vector",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT));

    public static final MethodHandle zvec_vector_query_set_output_fields = downcall("zvec_vector_query_set_output_fields",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, C_INT));

    public static final MethodHandle zvec_vector_query_set_query_params = downcall("zvec_vector_query_set_query_params",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_vector_query_set_sparse_vector_f32 = downcall("zvec_vector_query_set_sparse_vector_f32",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS, C_INT));

    // ========== Result containers ==========
    public static final MethodHandle zvec_doc_list_count = downcall("zvec_doc_list_count",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_list_get = downcall("zvec_doc_list_get",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT, ADDRESS));

    public static final MethodHandle zvec_doc_list_destroy = downcall("zvec_doc_list_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_doc_map_count = downcall("zvec_doc_map_count",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_map_keys = downcall("zvec_doc_map_keys",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_map_get = downcall("zvec_doc_map_get",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_doc_map_destroy = downcall("zvec_doc_map_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_write_results_count = downcall("zvec_write_results_count",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_write_results_status_code = downcall("zvec_write_results_status_code",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT, ADDRESS));

    public static final MethodHandle zvec_write_results_status_message = downcall("zvec_write_results_status_message",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT, ADDRESS));

    public static final MethodHandle zvec_write_results_destroy = downcall("zvec_write_results_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));

    public static final MethodHandle zvec_string_array_count = downcall("zvec_string_array_count",
            FunctionDescriptor.of(C_INT, ADDRESS, ADDRESS));

    public static final MethodHandle zvec_string_array_get = downcall("zvec_string_array_get",
            FunctionDescriptor.of(C_INT, ADDRESS, C_INT, ADDRESS));

    public static final MethodHandle zvec_string_array_destroy = downcall("zvec_string_array_destroy",
            FunctionDescriptor.of(C_INT, ADDRESS));
}
