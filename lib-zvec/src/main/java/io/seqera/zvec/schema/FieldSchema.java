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

package io.seqera.zvec.schema;

import io.seqera.zvec.internal.MemoryUtils;
import io.seqera.zvec.internal.ZvecBindings;
import io.seqera.zvec.param.IndexParam;
import io.seqera.zvec.type.DataType;
import io.seqera.zvec.type.IndexType;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class FieldSchema {
    private final String name;
    private final DataType dataType;
    private final boolean nullable;
    private final IndexParam indexParam;

    public FieldSchema(String name, DataType dataType) {
        this(name, dataType, false, null);
    }

    public FieldSchema(String name, DataType dataType, boolean nullable) {
        this(name, dataType, nullable, null);
    }

    public FieldSchema(String name, DataType dataType, boolean nullable, IndexParam indexParam) {
        this.name = name;
        this.dataType = dataType;
        this.nullable = nullable;
        this.indexParam = indexParam;
    }

    public String name() { return name; }
    public DataType dataType() { return dataType; }
    public boolean nullable() { return nullable; }
    public IndexParam indexParam() { return indexParam; }

    public MemorySegment createNative(Arena arena) {
        try {
            MemorySegment indexParamsHandle = MemorySegment.NULL;
            if (indexParam != null) {
                indexParamsHandle = indexParam.createNative(arena);
            }
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_field_schema_create.invokeExact(
                    MemoryUtils.toCString(arena, name),
                    dataType.value(),
                    nullable ? 1 : 0,
                    indexParamsHandle,
                    out);
            MemoryUtils.checkStatus(rc);
            return MemoryUtils.readOutPointer(out);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create native FieldSchema", t);
        }
    }

    public static FieldSchema fromNative(MemorySegment handle) {
        try (var arena = Arena.ofConfined()) {
            var nameOut = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_field_schema_name.invokeExact(handle, nameOut));
            String name = MemoryUtils.readAndFreeString(nameOut);

            var dtOut = MemoryUtils.allocateOutInt(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_field_schema_data_type.invokeExact(handle, dtOut));
            DataType dataType = DataType.fromValue(MemoryUtils.readOutInt(dtOut));

            var nullOut = MemoryUtils.allocateOutInt(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_field_schema_nullable.invokeExact(handle, nullOut));
            boolean nullable = MemoryUtils.readOutInt(nullOut) != 0;

            return new FieldSchema(name, dataType, nullable);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to read native FieldSchema", t);
        }
    }
}
