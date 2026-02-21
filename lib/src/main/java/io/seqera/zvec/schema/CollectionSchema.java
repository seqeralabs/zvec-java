package io.seqera.zvec.schema;

import io.seqera.zvec.internal.MemoryUtils;
import io.seqera.zvec.internal.ZvecBindings;
import io.seqera.zvec.type.DataType;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionSchema implements AutoCloseable {
    private final String name;
    private final List<FieldSchema> fields;
    private final List<VectorSchema> vectors;

    public CollectionSchema(String name, List<FieldSchema> fields, List<VectorSchema> vectors) {
        this.name = name;
        this.fields = fields != null ? List.copyOf(fields) : List.of();
        this.vectors = vectors != null ? List.copyOf(vectors) : List.of();
    }

    public String name() { return name; }
    public List<FieldSchema> fields() { return fields; }
    public List<VectorSchema> vectors() { return vectors; }

    public FieldSchema field(String name) {
        return fields.stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
    }

    public VectorSchema vector(String name) {
        return vectors.stream().filter(v -> v.name().equals(name)).findFirst().orElse(null);
    }

    public MemorySegment createNative(Arena arena) {
        try {
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_collection_schema_create.invokeExact(
                    MemoryUtils.toCString(arena, name), out);
            MemoryUtils.checkStatus(rc);
            var schemaHandle = MemoryUtils.readOutPointer(out);

            for (var field : fields) {
                var fieldHandle = field.createNative(arena);
                rc = (int) ZvecBindings.zvec_collection_schema_add_field.invokeExact(schemaHandle, fieldHandle);
                MemoryUtils.checkStatus(rc);
            }
            for (var vector : vectors) {
                var fieldHandle = vector.createNative(arena);
                rc = (int) ZvecBindings.zvec_collection_schema_add_field.invokeExact(schemaHandle, fieldHandle);
                MemoryUtils.checkStatus(rc);
            }
            return schemaHandle;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create native CollectionSchema", t);
        }
    }

    public static CollectionSchema fromNative(MemorySegment handle) {
        try (var arena = Arena.ofConfined()) {
            var nameOut = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_schema_name.invokeExact(handle, nameOut));
            String name = MemoryUtils.readAndFreeString(nameOut);

            var countOut = MemoryUtils.allocateOutInt(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_schema_field_count.invokeExact(handle, countOut));
            int count = MemoryUtils.readOutInt(countOut);

            var fieldList = new ArrayList<FieldSchema>();
            var vectorList = new ArrayList<VectorSchema>();

            for (int i = 0; i < count; i++) {
                var fieldOut = MemoryUtils.allocateOutPointer(arena);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_schema_field_at.invokeExact(handle, i, fieldOut));
                var fieldHandle = MemoryUtils.readOutPointer(fieldOut);

                var dtOut = MemoryUtils.allocateOutInt(arena);
                MemoryUtils.checkStatus((int) ZvecBindings.zvec_field_schema_data_type.invokeExact(fieldHandle, dtOut));
                DataType dt = DataType.fromValue(MemoryUtils.readOutInt(dtOut));

                if (dt.isVectorType()) {
                    vectorList.add(VectorSchema.fromNative(fieldHandle));
                } else {
                    fieldList.add(FieldSchema.fromNative(fieldHandle));
                }
                int _r = (int) ZvecBindings.zvec_field_schema_destroy.invokeExact(fieldHandle);
            }

            return new CollectionSchema(name, fieldList, vectorList);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to read native CollectionSchema", t);
        }
    }

    @Override
    public void close() {
        // No native handle to free — schema is a pure Java object after fromNative
    }
}
