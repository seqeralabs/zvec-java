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

/**
 * Demonstrates the variety of schema definition options available in Zvec, from simple single-vector
 * schemas to complex multi-field, multi-vector schemas with different data types and index strategies.
 *
 * <p>This example illustrates two schema configurations:
 * <ul>
 *   <li><b>Simple schema</b> — a single STRING field and one vector field with a brute-force flat index</li>
 *   <li><b>Complex schema</b> — multiple scalar fields of various types (STRING, FLOAT, INT32, BOOL,
 *       ARRAY_STRING) combined with multiple vector fields using different index algorithms and metrics</li>
 * </ul>
 *
 * <p>Key concepts covered:
 * <ul>
 *   <li>{@link DataType} — supported scalar types: STRING, FLOAT, INT32, BOOL, ARRAY_STRING</li>
 *   <li>{@link FlatIndexParam} — brute-force flat index for exact nearest-neighbor search</li>
 *   <li>{@link HnswIndexParam} — HNSW (Hierarchical Navigable Small World) graph-based index for
 *       fast approximate nearest-neighbor search; constructor takes the metric type, M (max connections
 *       per node), and efConstruction (build-time search breadth)</li>
 *   <li>{@link InvertIndexParam} — inverted index on a scalar field, enabling efficient filter-based queries</li>
 *   <li>{@link MetricType#COSINE} — cosine similarity, {@link MetricType#L2} — L2/Euclidean distance,
 *       {@link MetricType#IP} — inner product</li>
 *   <li>Nullable fields — setting the nullable flag to {@code true} allows a field to be omitted from documents</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>{@code ./gradlew :examples:SchemaExample}</pre>
 */
public class SchemaExample {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_schema");

        // Simple schema: one STRING field and one 128-dimensional vector field.
        // Uses a flat index (brute-force exact search) with cosine similarity.
        var simpleSchema = new CollectionSchema("simple",
                List.of(new FieldSchema("title", DataType.STRING)),
                List.of(new VectorSchema("emb", DataType.VECTOR_FP32, 128,
                        new FlatIndexParam(MetricType.COSINE))));

        // Complex schema demonstrating the full range of schema options:
        //
        // Scalar fields:
        //  - "title"       : non-nullable STRING with an inverted index for efficient filtering
        //  - "description" : nullable STRING (can be omitted when inserting documents)
        //  - "price"       : FLOAT for numeric values
        //  - "count"       : INT32 for integer values
        //  - "active"      : BOOL for boolean flags
        //  - "tags"        : nullable ARRAY_STRING for multi-valued string fields
        //
        // Vector fields:
        //  - "dense_emb"   : 256-dim FP32 vector with an HNSW index (M=32, efConstruction=200)
        //                    using cosine similarity — good for large-scale approximate search
        //  - "image_emb"   : 512-dim FP32 vector with a flat index using L2 (Euclidean) distance
        //                    — exact search, suitable for smaller datasets or recall-critical use cases
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

        // Create and open the complex collection, then print its schema summary.
        try (var coll = Zvec.createAndOpen(tempDir.resolve("complex_coll").toString(), complexSchema)) {
            System.out.println("Created collection with schema: " + coll.schema().name());
            System.out.println("Fields: " + coll.schema().fields().size());
            System.out.println("Vectors: " + coll.schema().vectors().size());
            // Destroy removes all on-disk data; closing alone only releases the native handle.
            coll.destroy();
        }
    }
}
