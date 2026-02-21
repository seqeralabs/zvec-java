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

public class IndexManagementExample {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_index");
        var schema = new CollectionSchema("index_demo",
                List.of(
                        new FieldSchema("title", DataType.STRING),
                        new FieldSchema("category", DataType.STRING)
                ),
                List.of(new VectorSchema("emb", DataType.VECTOR_FP32, 4,
                        new FlatIndexParam(MetricType.COSINE))));

        try (var coll = Zvec.createAndOpen(tempDir.resolve("index_coll").toString(), schema)) {
            // Insert some data
            for (int i = 1; i <= 100; i++) {
                coll.insert(new Doc("doc" + i)
                        .field("title", "Document " + i)
                        .field("category", "cat" + (i % 5))
                        .vector("emb", new float[]{i * 0.01f, i * 0.02f, i * 0.03f, i * 0.04f}));
            }

            // Create HNSW index on vector field
            System.out.println("Creating HNSW index...");
            coll.createIndex("emb", new HnswIndexParam(MetricType.COSINE, 32, 200));
            System.out.println("HNSW index created");

            // Create invert index on scalar field
            System.out.println("Creating invert index on 'category'...");
            coll.createIndex("category", new InvertIndexParam());
            System.out.println("Invert index created");

            // Optimize
            System.out.println("Optimizing...");
            coll.optimize();
            System.out.println("Optimization complete");

            // Drop index
            System.out.println("Dropping HNSW index...");
            coll.dropIndex("emb");
            System.out.println("Index dropped");

            coll.destroy();
        }
    }
}
