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

package io.seqera.zvec;

import io.seqera.zvec.internal.MemoryUtils;
import io.seqera.zvec.internal.ZvecBindings;
import io.seqera.zvec.param.*;
import io.seqera.zvec.schema.*;
import io.seqera.zvec.type.DataType;
import io.seqera.zvec.type.StatusCode;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.*;

public class Collection implements AutoCloseable {
    private MemorySegment handle;
    private CollectionSchema schema;
    private boolean closed = false;

    Collection(MemorySegment handle, CollectionSchema schema) {
        this.handle = handle;
        this.schema = schema;
    }

    // ========== Properties ==========

    public String path() {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            var out = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_path.invokeExact(handle, out));
            return MemoryUtils.readAndFreeString(out);
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get collection path", t);
        }
    }

    public CollectionSchema schema() {
        return schema;
    }

    public CollectionStats stats() {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            var out = MemoryUtils.allocateOutLong(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_stats.invokeExact(handle, out));
            return new CollectionStats(MemoryUtils.readOutLong(out));
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get collection stats", t);
        }
    }

    // ========== Lifecycle ==========

    public void destroy() {
        if (closed) return;
        try {
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_destroy.invokeExact(handle));
            closed = true;
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to destroy collection", t);
        }
    }

    public void flush() {
        ensureOpen();
        try {
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_flush.invokeExact(handle));
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to flush collection", t);
        }
    }

    @Override
    public void close() {
        if (closed) return;
        try {
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_close.invokeExact(handle));
            closed = true;
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to close collection", t);
        }
    }

    // ========== Index DDL ==========

    public void createIndex(String fieldName, IndexParam indexParam) {
        createIndex(fieldName, indexParam, new IndexOption());
    }

    public void createIndex(String fieldName, IndexParam indexParam, IndexOption option) {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            var ipHandle = indexParam.createNative(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_create_index.invokeExact(
                    handle, MemoryUtils.toCString(arena, fieldName), ipHandle, option.concurrency()));
            refreshSchema();
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create index", t);
        }
    }

    public void dropIndex(String fieldName) {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_drop_index.invokeExact(
                    handle, MemoryUtils.toCString(arena, fieldName)));
            refreshSchema();
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to drop index", t);
        }
    }

    public void optimize() {
        optimize(new OptimizeOption());
    }

    public void optimize(OptimizeOption option) {
        ensureOpen();
        try {
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_optimize.invokeExact(
                    handle, option.concurrency()));
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to optimize collection", t);
        }
    }

    // ========== Column DDL ==========

    public void addColumn(FieldSchema fieldSchema, String expression) {
        addColumn(fieldSchema, expression, new AddColumnOption());
    }

    public void addColumn(FieldSchema fieldSchema, String expression, AddColumnOption option) {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            var fsHandle = fieldSchema.createNative(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_add_column.invokeExact(
                    handle, fsHandle, MemoryUtils.toCString(arena, expression), option.concurrency()));
            refreshSchema();
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to add column", t);
        }
    }

    public void dropColumn(String fieldName) {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_drop_column.invokeExact(
                    handle, MemoryUtils.toCString(arena, fieldName)));
            refreshSchema();
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to drop column", t);
        }
    }

    public void alterColumn(String oldName, String newName) {
        alterColumn(oldName, newName, null, new AlterColumnOption());
    }

    public void alterColumn(String oldName, String newName, FieldSchema newSchema, AlterColumnOption option) {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            MemorySegment newSchemaHandle = MemorySegment.NULL;
            if (newSchema != null) {
                newSchemaHandle = newSchema.createNative(arena);
            }
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_alter_column.invokeExact(
                    handle,
                    MemoryUtils.toCString(arena, oldName),
                    MemoryUtils.toCString(arena, newName != null ? newName : ""),
                    newSchemaHandle,
                    option.concurrency()));
            refreshSchema();
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to alter column", t);
        }
    }

    // ========== DML ==========

    public StatusCode insert(Doc doc) {
        return insert(List.of(doc)).get(0);
    }

    public List<StatusCode> insert(List<Doc> docs) {
        return executeDml(docs, ZvecBindings.zvec_collection_insert);
    }

    public StatusCode upsert(Doc doc) {
        return upsert(List.of(doc)).get(0);
    }

    public List<StatusCode> upsert(List<Doc> docs) {
        return executeDml(docs, ZvecBindings.zvec_collection_upsert);
    }

    public StatusCode update(Doc doc) {
        return update(List.of(doc)).get(0);
    }

    public List<StatusCode> update(List<Doc> docs) {
        return executeDml(docs, ZvecBindings.zvec_collection_update);
    }

    public StatusCode delete(String id) {
        return delete(List.of(id)).get(0);
    }

    public List<StatusCode> delete(List<String> ids) {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            var pksArray = MemoryUtils.toStringArray(arena, ids.toArray(new String[0]));
            var out = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_delete.invokeExact(
                    handle, pksArray, ids.size(), out));
            return readWriteResults(MemoryUtils.readOutPointer(out));
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to delete documents", t);
        }
    }

    public void deleteByFilter(String filter) {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_delete_by_filter.invokeExact(
                    handle, MemoryUtils.toCString(arena, filter)));
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to delete by filter", t);
        }
    }

    // ========== DQL ==========

    public List<Doc> query(VectorQuery vectorQuery, int topk) {
        return query(vectorQuery, topk, null, false, null);
    }

    public List<Doc> query(VectorQuery vectorQuery, int topk, String filter,
                           boolean includeVector, String[] outputFields) {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            var queryHandle = vectorQuery.createNative(arena, topk, filter, includeVector, outputFields);
            var out = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_query.invokeExact(
                    handle, queryHandle, out));
            var listHandle = MemoryUtils.readOutPointer(out);

            try {
                return readDocList(listHandle, arena);
            } finally {
                int _r1 = (int) ZvecBindings.zvec_doc_list_destroy.invokeExact(listHandle);
            }
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to query collection", t);
        }
    }

    public Map<String, Doc> fetch(String id) {
        return fetch(List.of(id));
    }

    public Map<String, Doc> fetch(List<String> ids) {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            var pksArray = MemoryUtils.toStringArray(arena, ids.toArray(new String[0]));
            var out = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_fetch.invokeExact(
                    handle, pksArray, ids.size(), out));
            var mapHandle = MemoryUtils.readOutPointer(out);

            try {
                return readDocMap(mapHandle, arena);
            } finally {
                int _r2 = (int) ZvecBindings.zvec_doc_map_destroy.invokeExact(mapHandle);
            }
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to fetch documents", t);
        }
    }

    // ========== Private helpers ==========

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Collection is closed");
    }

    private void refreshSchema() {
        try (var arena = Arena.ofConfined()) {
            var out = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_schema.invokeExact(handle, out));
            var schemaHandle = MemoryUtils.readOutPointer(out);
            this.schema = CollectionSchema.fromNative(schemaHandle);
            int _r3 = (int) ZvecBindings.zvec_collection_schema_destroy.invokeExact(schemaHandle);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to refresh schema", t);
        }
    }

    private List<StatusCode> executeDml(List<Doc> docs, java.lang.invoke.MethodHandle mh) {
        ensureOpen();
        try (var arena = Arena.ofConfined()) {
            var docHandles = new MemorySegment[docs.size()];
            for (int i = 0; i < docs.size(); i++) {
                docHandles[i] = docToNative(arena, docs.get(i));
            }
            var docsArray = MemoryUtils.toPointerArray(arena, docHandles);
            var out = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) mh.invokeExact(handle, docsArray, docs.size(), out));
            var results = readWriteResults(MemoryUtils.readOutPointer(out));

            for (var dh : docHandles) {
                int _r4 = (int) ZvecBindings.zvec_doc_destroy.invokeExact(dh);
            }
            return results;
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to execute DML", t);
        }
    }

    private MemorySegment docToNative(Arena arena, Doc doc) throws Throwable {
        var out = MemoryUtils.allocateOutPointer(arena);
        MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_create.invokeExact(out));
        var docHandle = MemoryUtils.readOutPointer(out);

        MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_pk.invokeExact(
                docHandle, MemoryUtils.toCString(arena, doc.id())));

        // Set fields
        for (var entry : doc.fields().entrySet()) {
            var name = MemoryUtils.toCString(arena, entry.getKey());
            var value = entry.getValue();
            if (value == null) {
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_null.invokeExact(docHandle, name));
            } else if (value instanceof CharSequence cs) {
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_string.invokeExact(
                        docHandle, name, MemoryUtils.toCString(arena, cs.toString())));
            } else if (value instanceof Integer v) {
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_int32.invokeExact(docHandle, name, v.intValue()));
            } else if (value instanceof Long v) {
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_int64.invokeExact(docHandle, name, v.longValue()));
            } else if (value instanceof Float v) {
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_float.invokeExact(docHandle, name, v.floatValue()));
            } else if (value instanceof Double v) {
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_double.invokeExact(docHandle, name, v.doubleValue()));
            } else if (value instanceof Boolean v) {
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_bool.invokeExact(docHandle, name, v ? 1 : 0));
            } else if (value instanceof String[] arr) {
                var strArr = MemoryUtils.toStringArray(arena, arr);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_string_array.invokeExact(
                        docHandle, name, strArr, arr.length));
            } else if (value instanceof int[] arr) {
                var seg = MemoryUtils.toIntArray(arena, arr);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_int32_array.invokeExact(
                        docHandle, name, seg, arr.length));
            } else if (value instanceof long[] arr) {
                var seg = MemoryUtils.toLongArray(arena, arr);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_int64_array.invokeExact(
                        docHandle, name, seg, arr.length));
            } else if (value instanceof float[] arr) {
                var seg = MemoryUtils.toFloatArray(arena, arr);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_float_array.invokeExact(
                        docHandle, name, seg, arr.length));
            } else if (value instanceof double[] arr) {
                var seg = MemoryUtils.toDoubleArray(arena, arr);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_double_array.invokeExact(
                        docHandle, name, seg, arr.length));
            }
        }

        // Set vectors
        for (var entry : doc.vectors().entrySet()) {
            var name = MemoryUtils.toCString(arena, entry.getKey());
            var value = entry.getValue();
            if (value instanceof float[] arr) {
                var seg = MemoryUtils.toFloatArray(arena, arr);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_vector_f32.invokeExact(
                        docHandle, name, seg, arr.length));
            } else if (value instanceof double[] arr) {
                var seg = MemoryUtils.toDoubleArray(arena, arr);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_vector_f64.invokeExact(
                        docHandle, name, seg, arr.length));
            } else if (value instanceof Doc.SparseVector sv) {
                var idxSeg = MemoryUtils.toIntArray(arena, sv.indices());
                var valSeg = MemoryUtils.toFloatArray(arena, sv.values());
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_set_sparse_vector_f32.invokeExact(
                        docHandle, name, idxSeg, valSeg, sv.indices().length));
            }
        }

        return docHandle;
    }

    private List<StatusCode> readWriteResults(MemorySegment resultsHandle) throws Throwable {
        try {
            var countOut = Arena.ofAuto().allocate(ValueLayout.JAVA_INT);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_write_results_count.invokeExact(resultsHandle, countOut));
            int count = countOut.get(ValueLayout.JAVA_INT, 0);

            var results = new ArrayList<StatusCode>(count);
            for (int i = 0; i < count; i++) {
                var codeOut = Arena.ofAuto().allocate(ValueLayout.JAVA_INT);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_write_results_status_code.invokeExact(
                        resultsHandle, i, codeOut));
                results.add(StatusCode.fromValue(codeOut.get(ValueLayout.JAVA_INT, 0)));
            }
            return results;
        } finally {
            int ignored = (int) ZvecBindings.zvec_write_results_destroy.invokeExact(resultsHandle);
        }
    }

    private List<Doc> readDocList(MemorySegment listHandle, Arena arena) throws Throwable {
        var countOut = MemoryUtils.allocateOutInt(arena);
        MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_list_count.invokeExact(listHandle, countOut));
        int count = MemoryUtils.readOutInt(countOut);

        var docs = new ArrayList<Doc>(count);
        for (int i = 0; i < count; i++) {
            var docOut = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_list_get.invokeExact(listHandle, i, docOut));
            var docHandle = MemoryUtils.readOutPointer(docOut);
            docs.add(nativeToDoc(docHandle, arena));
            int _r5 = (int) ZvecBindings.zvec_doc_destroy.invokeExact(docHandle);
        }
        return docs;
    }

    private Map<String, Doc> readDocMap(MemorySegment mapHandle, Arena arena) throws Throwable {
        var keysOut = MemoryUtils.allocateOutPointer(arena);
        MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_map_keys.invokeExact(mapHandle, keysOut));
        var keysHandle = MemoryUtils.readOutPointer(keysOut);

        var keysCountOut = MemoryUtils.allocateOutInt(arena);
        MemoryUtils.checkStatus((int) ZvecBindings.zvec_string_array_count.invokeExact(keysHandle, keysCountOut));
        int keyCount = MemoryUtils.readOutInt(keysCountOut);

        var result = new LinkedHashMap<String, Doc>();
        for (int i = 0; i < keyCount; i++) {
            var keyOut = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_string_array_get.invokeExact(keysHandle, i, keyOut));
            String key = MemoryUtils.readAndFreeString(keyOut);

            var docOut = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_map_get.invokeExact(
                    mapHandle, MemoryUtils.toCString(arena, key), docOut));
            var docHandle = MemoryUtils.readOutPointer(docOut);
            result.put(key, nativeToDoc(docHandle, arena));
            int _r5 = (int) ZvecBindings.zvec_doc_destroy.invokeExact(docHandle);
        }
        int _r6 = (int) ZvecBindings.zvec_string_array_destroy.invokeExact(keysHandle);
        return result;
    }

    private Doc nativeToDoc(MemorySegment docHandle, Arena arena) throws Throwable {
        // Read pk
        var pkOut = MemoryUtils.allocateOutPointer(arena);
        MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_get_pk.invokeExact(docHandle, pkOut));
        String pk = MemoryUtils.readAndFreeString(pkOut);

        // Read score
        var scoreOut = MemoryUtils.allocateOutFloat(arena);
        MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_get_score.invokeExact(docHandle, scoreOut));
        float score = MemoryUtils.readOutFloat(scoreOut);

        Doc doc = new Doc(pk);
        doc.setScore(score);

        // Read all fields using field_names
        var namesOut = MemoryUtils.allocateOutPointer(arena);
        MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_field_names.invokeExact(docHandle, namesOut));
        var namesHandle = MemoryUtils.readOutPointer(namesOut);

        var namesCountOut = MemoryUtils.allocateOutInt(arena);
        MemoryUtils.checkStatus((int) ZvecBindings.zvec_string_array_count.invokeExact(namesHandle, namesCountOut));
        int nameCount = MemoryUtils.readOutInt(namesCountOut);

        for (int i = 0; i < nameCount; i++) {
            var nameOut = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_string_array_get.invokeExact(namesHandle, i, nameOut));
            String fieldName = MemoryUtils.readAndFreeString(nameOut);

            // Determine if this is a vector field or scalar field from schema
            boolean isVectorField = false;
            DataType dt = null;
            if (schema != null) {
                var vs = schema.vector(fieldName);
                if (vs != null) {
                    isVectorField = true;
                    dt = vs.dataType();
                } else {
                    var fs = schema.field(fieldName);
                    if (fs != null) dt = fs.dataType();
                }
            }

            if (isVectorField && dt != null) {
                readVectorField(doc, docHandle, fieldName, dt, arena);
            } else {
                readScalarField(doc, docHandle, fieldName, dt, arena);
            }
        }
        int _r7 = (int) ZvecBindings.zvec_string_array_destroy.invokeExact(namesHandle);

        return doc;
    }

    private void readVectorField(Doc doc, MemorySegment docHandle, String fieldName,
                                  DataType dt, Arena arena) throws Throwable {
        var nameCs = MemoryUtils.toCString(arena, fieldName);
        if (dt == DataType.VECTOR_FP32) {
            var ptrOut = MemoryUtils.allocateOutPointer(arena);
            var dimOut = MemoryUtils.allocateOutInt(arena);
            int rc = (int) ZvecBindings.zvec_doc_get_vector_f32.invokeExact(docHandle, nameCs, ptrOut, dimOut);
            if (rc == 0) {
                int dim = MemoryUtils.readOutInt(dimOut);
                var ptr = MemoryUtils.readOutPointer(ptrOut);
                doc.vector(fieldName, MemoryUtils.fromFloatArray(ptr, dim));
            }
        } else if (dt == DataType.VECTOR_FP64) {
            var ptrOut = MemoryUtils.allocateOutPointer(arena);
            var dimOut = MemoryUtils.allocateOutInt(arena);
            int rc = (int) ZvecBindings.zvec_doc_get_vector_f64.invokeExact(docHandle, nameCs, ptrOut, dimOut);
            if (rc == 0) {
                int dim = MemoryUtils.readOutInt(dimOut);
                var ptr = MemoryUtils.readOutPointer(ptrOut);
                doc.vector(fieldName, MemoryUtils.fromDoubleArray(ptr, dim));
            }
        } else if (dt == DataType.SPARSE_VECTOR_FP32) {
            var idxOut = MemoryUtils.allocateOutPointer(arena);
            var valOut = MemoryUtils.allocateOutPointer(arena);
            var cntOut = MemoryUtils.allocateOutInt(arena);
            int rc = (int) ZvecBindings.zvec_doc_get_sparse_vector_f32.invokeExact(
                    docHandle, nameCs, idxOut, valOut, cntOut);
            if (rc == 0) {
                int cnt = MemoryUtils.readOutInt(cntOut);
                var idxPtr = MemoryUtils.readOutPointer(idxOut);
                var valPtr = MemoryUtils.readOutPointer(valOut);
                doc.sparseVector(fieldName,
                        MemoryUtils.fromIntArray(idxPtr, cnt),
                        MemoryUtils.fromFloatArray(valPtr, cnt));
            }
        }
    }

    private void readScalarField(Doc doc, MemorySegment docHandle, String fieldName,
                                  DataType dt, Arena arena) throws Throwable {
        var nameCs = MemoryUtils.toCString(arena, fieldName);

        // Check if null
        var nullOut = MemoryUtils.allocateOutInt(arena);
        MemoryUtils.checkStatus((int) ZvecBindings.zvec_doc_is_null.invokeExact(docHandle, nameCs, nullOut));
        if (MemoryUtils.readOutInt(nullOut) != 0) {
            doc.nullField(fieldName);
            return;
        }

        if (dt == null) {
            // Try string as fallback
            tryReadString(doc, docHandle, fieldName, nameCs, arena);
            return;
        }

        switch (dt) {
            case STRING -> tryReadString(doc, docHandle, fieldName, nameCs, arena);
            case INT32 -> {
                var out = MemoryUtils.allocateOutInt(arena);
                if ((int) ZvecBindings.zvec_doc_get_int32.invokeExact(docHandle, nameCs, out) == 0)
                    doc.field(fieldName, MemoryUtils.readOutInt(out));
            }
            case INT64 -> {
                var out = MemoryUtils.allocateOutLong(arena);
                if ((int) ZvecBindings.zvec_doc_get_int64.invokeExact(docHandle, nameCs, out) == 0)
                    doc.field(fieldName, MemoryUtils.readOutLong(out));
            }
            case UINT32 -> {
                var out = MemoryUtils.allocateOutInt(arena);
                if ((int) ZvecBindings.zvec_doc_get_uint32.invokeExact(docHandle, nameCs, out) == 0)
                    doc.field(fieldName, MemoryUtils.readOutInt(out));
            }
            case UINT64 -> {
                var out = MemoryUtils.allocateOutLong(arena);
                if ((int) ZvecBindings.zvec_doc_get_uint64.invokeExact(docHandle, nameCs, out) == 0)
                    doc.field(fieldName, MemoryUtils.readOutLong(out));
            }
            case FLOAT -> {
                var out = MemoryUtils.allocateOutFloat(arena);
                if ((int) ZvecBindings.zvec_doc_get_float.invokeExact(docHandle, nameCs, out) == 0)
                    doc.field(fieldName, MemoryUtils.readOutFloat(out));
            }
            case DOUBLE -> {
                var out = arena.allocate(ValueLayout.JAVA_DOUBLE);
                if ((int) ZvecBindings.zvec_doc_get_double.invokeExact(docHandle, nameCs, out) == 0)
                    doc.field(fieldName, out.get(ValueLayout.JAVA_DOUBLE, 0));
            }
            case BOOL -> {
                var out = MemoryUtils.allocateOutInt(arena);
                if ((int) ZvecBindings.zvec_doc_get_bool.invokeExact(docHandle, nameCs, out) == 0)
                    doc.field(fieldName, MemoryUtils.readOutInt(out) != 0);
            }
            case ARRAY_STRING -> {
                var out = MemoryUtils.allocateOutPointer(arena);
                if ((int) ZvecBindings.zvec_doc_get_string_array.invokeExact(docHandle, nameCs, out) == 0) {
                    var saHandle = MemoryUtils.readOutPointer(out);
                    doc.field(fieldName, readStringArray(saHandle));
                }
            }
            case ARRAY_INT32 -> {
                var pOut = MemoryUtils.allocateOutPointer(arena);
                var cOut = MemoryUtils.allocateOutInt(arena);
                if ((int) ZvecBindings.zvec_doc_get_int32_array.invokeExact(docHandle, nameCs, pOut, cOut) == 0) {
                    doc.field(fieldName, MemoryUtils.fromIntArray(MemoryUtils.readOutPointer(pOut), MemoryUtils.readOutInt(cOut)));
                }
            }
            case ARRAY_INT64 -> {
                var pOut = MemoryUtils.allocateOutPointer(arena);
                var cOut = MemoryUtils.allocateOutInt(arena);
                if ((int) ZvecBindings.zvec_doc_get_int64_array.invokeExact(docHandle, nameCs, pOut, cOut) == 0) {
                    doc.field(fieldName, MemoryUtils.fromLongArray(MemoryUtils.readOutPointer(pOut), MemoryUtils.readOutInt(cOut)));
                }
            }
            case ARRAY_FLOAT -> {
                var pOut = MemoryUtils.allocateOutPointer(arena);
                var cOut = MemoryUtils.allocateOutInt(arena);
                if ((int) ZvecBindings.zvec_doc_get_float_array.invokeExact(docHandle, nameCs, pOut, cOut) == 0) {
                    doc.field(fieldName, MemoryUtils.fromFloatArray(MemoryUtils.readOutPointer(pOut), MemoryUtils.readOutInt(cOut)));
                }
            }
            case ARRAY_DOUBLE -> {
                var pOut = MemoryUtils.allocateOutPointer(arena);
                var cOut = MemoryUtils.allocateOutInt(arena);
                if ((int) ZvecBindings.zvec_doc_get_double_array.invokeExact(docHandle, nameCs, pOut, cOut) == 0) {
                    doc.field(fieldName, MemoryUtils.fromDoubleArray(MemoryUtils.readOutPointer(pOut), MemoryUtils.readOutInt(cOut)));
                }
            }
            default -> tryReadString(doc, docHandle, fieldName, nameCs, arena);
        }
    }

    private void tryReadString(Doc doc, MemorySegment docHandle, String fieldName,
                                MemorySegment nameCs, Arena arena) throws Throwable {
        var out = MemoryUtils.allocateOutPointer(arena);
        if ((int) ZvecBindings.zvec_doc_get_string.invokeExact(docHandle, nameCs, out) == 0) {
            doc.field(fieldName, MemoryUtils.readAndFreeString(out));
        }
    }

    private String[] readStringArray(MemorySegment saHandle) throws Throwable {
        try (var arena = Arena.ofConfined()) {
            var countOut = MemoryUtils.allocateOutInt(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_string_array_count.invokeExact(saHandle, countOut));
            int count = MemoryUtils.readOutInt(countOut);
            var result = new String[count];
            for (int i = 0; i < count; i++) {
                var out = MemoryUtils.allocateOutPointer(arena);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_string_array_get.invokeExact(saHandle, i, out));
                result[i] = MemoryUtils.readAndFreeString(out);
            }
            return result;
        } finally {
            int _r8 = (int) ZvecBindings.zvec_string_array_destroy.invokeExact(saHandle);
        }
    }
}
