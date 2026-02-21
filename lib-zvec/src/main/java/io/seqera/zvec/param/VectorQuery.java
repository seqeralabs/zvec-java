/*
 * Copyright 2026, Seqera Labs
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

package io.seqera.zvec.param;

import io.seqera.zvec.internal.MemoryUtils;
import io.seqera.zvec.internal.ZvecBindings;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Map;

public class VectorQuery {
    private final String fieldName;
    private float[] vector;
    private int[] sparseIndices;
    private float[] sparseValues;
    private QueryParam queryParam;

    public VectorQuery(String fieldName, float[] vector) {
        this.fieldName = fieldName;
        this.vector = vector;
    }

    public VectorQuery(String fieldName, int[] sparseIndices, float[] sparseValues) {
        this.fieldName = fieldName;
        this.sparseIndices = sparseIndices;
        this.sparseValues = sparseValues;
    }

    public VectorQuery queryParam(QueryParam queryParam) {
        this.queryParam = queryParam;
        return this;
    }

    public String fieldName() { return fieldName; }
    public float[] vector() { return vector; }
    public int[] sparseIndices() { return sparseIndices; }
    public float[] sparseValues() { return sparseValues; }
    public QueryParam queryParam() { return queryParam; }

    public MemorySegment createNative(Arena arena, int topk, String filter,
                                       boolean includeVector, String[] outputFields) {
        try {
            var out = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_vector_query_create.invokeExact(out));
            var handle = MemoryUtils.readOutPointer(out);

            MemoryUtils.checkStatus((int) ZvecBindings.zvec_vector_query_set_field_name.invokeExact(
                    handle, MemoryUtils.toCString(arena, fieldName)));

            MemoryUtils.checkStatus((int) ZvecBindings.zvec_vector_query_set_topk.invokeExact(handle, topk));

            if (vector != null) {
                var vecSeg = MemoryUtils.toFloatArray(arena, vector);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_vector_query_set_vector_f32.invokeExact(
                        handle, vecSeg, vector.length));
            }

            if (sparseIndices != null && sparseValues != null) {
                var idxSeg = MemoryUtils.toIntArray(arena, sparseIndices);
                var valSeg = MemoryUtils.toFloatArray(arena, sparseValues);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_vector_query_set_sparse_vector_f32.invokeExact(
                        handle, idxSeg, valSeg, sparseIndices.length));
            }

            if (filter != null) {
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_vector_query_set_filter.invokeExact(
                        handle, MemoryUtils.toCString(arena, filter)));
            }

            MemoryUtils.checkStatus((int) ZvecBindings.zvec_vector_query_set_include_vector.invokeExact(
                    handle, includeVector ? 1 : 0));

            if (outputFields != null) {
                var fieldsSeg = MemoryUtils.toStringArray(arena, outputFields);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_vector_query_set_output_fields.invokeExact(
                        handle, fieldsSeg, outputFields.length));
            }

            if (queryParam != null) {
                var qpHandle = queryParam.createNative(arena);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_vector_query_set_query_params.invokeExact(
                        handle, qpHandle));
            }

            return handle;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create native VectorQuery", t);
        }
    }
}
