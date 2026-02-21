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

package io.seqera.zvec.examples;

import io.seqera.zvec.*;
import io.seqera.zvec.param.*;
import io.seqera.zvec.schema.*;
import io.seqera.zvec.type.*;

import java.nio.file.Files;
import java.util.List;

public class SchemaExample {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_schema");

        // Single vector field schema
        var simpleSchema = new CollectionSchema("simple",
                List.of(new FieldSchema("title", DataType.STRING)),
                List.of(new VectorSchema("emb", DataType.VECTOR_FP32, 128,
                        new FlatIndexParam(MetricType.COSINE))));

        // Multi-field schema with various data types and index params
        var complexSchema = new CollectionSchema("complex",
                List.of(
                        new FieldSchema("title", DataType.STRING, false, new InvertIndexParam()),
                        new FieldSchema("description", DataType.STRING, true),
                        new FieldSchema("price", DataType.FLOAT),
                        new FieldSchema("count", DataType.INT32),
                        new FieldSchema("active", DataType.BOOL),
                        new FieldSchema("tags", DataType.ARRAY_STRING, true)
                ),
                List.of(
                        new VectorSchema("dense_emb", DataType.VECTOR_FP32, 256,
                                new HnswIndexParam(MetricType.COSINE, 32, 200)),
                        new VectorSchema("image_emb", DataType.VECTOR_FP32, 512,
                                new FlatIndexParam(MetricType.L2))
                ));

        try (var coll = Zvec.createAndOpen(tempDir.resolve("complex_coll").toString(), complexSchema)) {
            System.out.println("Created collection with schema: " + coll.schema().name());
            System.out.println("Fields: " + coll.schema().fields().size());
            System.out.println("Vectors: " + coll.schema().vectors().size());
            coll.destroy();
        }
    }
}
