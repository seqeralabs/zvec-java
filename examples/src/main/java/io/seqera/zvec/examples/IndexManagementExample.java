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
 * Demonstrates index lifecycle management in the zvec vector database.
 *
 * <p>This example covers the full index lifecycle:</p>
 * <ul>
 *   <li><b>HNSW index creation</b> -- build an HNSW (Hierarchical Navigable Small World)
 *       graph-based index on a vector field for fast approximate nearest neighbor search.
 *       HNSW params: M=32 (max connections per node in the graph), efConstruction=200
 *       (search width during index build; higher values improve recall at the cost of
 *       build time).</li>
 *   <li><b>Inverted index creation</b> -- build an inverted index on a scalar field to
 *       accelerate filter-based queries (e.g., filtering by category during vector search)</li>
 *   <li><b>Collection optimization</b> -- compact and optimize the collection storage for
 *       better query performance</li>
 *   <li><b>Index dropping</b> -- remove an index when it is no longer needed, reverting
 *       the field to unindexed state</li>
 * </ul>
 *
 * <p>The collection is initially created with a brute-force flat index. The HNSW index is
 * then created on top of the existing data, replacing the flat index with a graph-based
 * structure that enables much faster approximate nearest neighbor queries on large
 * datasets.</p>
 *
 * <p><b>Run with:</b> {@code ./gradlew :examples:run -PmainClass=io.seqera.zvec.examples.IndexManagementExample}</p>
 */
public class IndexManagementExample {
    public static void main(String[] args) throws Exception {
        // Initialize the zvec native library -- must be called before any other zvec operations
        Zvec.init();

        // Create a temporary directory for the on-disk collection storage
        var tempDir = Files.createTempDirectory("zvec_index");

        // Define the collection schema with two scalar fields and one 4-dimensional vector
        // field. The vector field starts with a brute-force flat index using cosine similarity.
        var schema = new CollectionSchema("index_demo",
                List.of(
                        new FieldSchema("title", DataType.STRING),
                        new FieldSchema("category", DataType.STRING)
                ),
                List.of(new VectorSchema("emb", DataType.VECTOR_FP32, 4,
                        new FlatIndexParam(MetricType.COSINE))));

        // Create and open the collection. The collection implements AutoCloseable, so
        // the try-with-resources block ensures the handle is released when done.
        try (var coll = Zvec.createAndOpen(tempDir.resolve("index_coll").toString(), schema)) {
            // Insert 100 documents using the fluent Doc builder. Each document has a unique ID,
            // scalar fields (title, category), and a 4-dimensional embedding vector.
            // Categories cycle through cat0-cat4 to demonstrate inverted index filtering.
            for (int i = 1; i <= 100; i++) {
                coll.insert(new Doc("doc" + i)
                        .field("title", "Document " + i)
                        .field("category", "cat" + (i % 5))
                        .vector("emb", new float[]{i * 0.01f, i * 0.02f, i * 0.03f, i * 0.04f}));
            }

            // Create an HNSW (Hierarchical Navigable Small World) index on the vector field.
            // This replaces the initial flat index with a graph-based structure for fast
            // approximate nearest neighbor search.
            // - M=32: maximum number of connections per node in the graph (higher M improves
            //   recall but increases memory usage and build time)
            // - efConstruction=200: search width during index construction (higher values
            //   produce a higher-quality graph at the cost of longer build times)
            System.out.println("Creating HNSW index...");
            coll.createIndex("emb", new HnswIndexParam(MetricType.COSINE, 32, 200));
            System.out.println("HNSW index created");

            // Create an inverted index on the "category" scalar field. This enables
            // efficient filtering during vector queries (e.g., restricting results to
            // a specific category). Without an inverted index, filter evaluation
            // requires a full scan of the scalar field values.
            System.out.println("Creating invert index on 'category'...");
            coll.createIndex("category", new InvertIndexParam());
            System.out.println("Invert index created");

            // Optimize the collection to compact storage and improve query performance.
            // This merges internal segments and reclaims space from deleted documents.
            System.out.println("Optimizing...");
            coll.optimize();
            System.out.println("Optimization complete");

            // Drop the HNSW index from the vector field. After dropping, queries on
            // this field will fall back to brute-force search until a new index is created.
            System.out.println("Dropping HNSW index...");
            coll.dropIndex("emb");
            System.out.println("Index dropped");

            // Destroy the collection to clean up all on-disk data. Note that close() (called
            // automatically by try-with-resources) only releases the native handle; destroy()
            // is needed to actually delete the persisted collection data from disk.
            coll.destroy();
        }
    }
}
