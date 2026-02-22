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

import java.nio.file.Files;
import java.util.List;

import io.seqera.zvec.Doc;
import io.seqera.zvec.Zvec;
import io.seqera.zvec.param.FlatIndexParam;
import io.seqera.zvec.param.HnswIndexParam;
import io.seqera.zvec.param.VectorQuery;
import io.seqera.zvec.schema.CollectionSchema;
import io.seqera.zvec.schema.FieldSchema;
import io.seqera.zvec.schema.VectorSchema;
import io.seqera.zvec.type.DataType;
import io.seqera.zvec.type.MetricType;

/**
 * Demonstrates various query modes available in the zvec vector database.
 *
 * <p>This example covers the following query capabilities:</p>
 * <ul>
 *   <li><b>Basic similarity search</b> -- find the most similar documents to a query vector,
 *       ranked by cosine similarity</li>
 *   <li><b>Filtered query</b> -- restrict results using SQL-like filter expressions on
 *       scalar fields (zvec uses SQL-like {@code =} for equality in filters)</li>
 *   <li><b>Output field selection</b> -- limit which scalar fields are returned in the
 *       result documents to reduce data transfer</li>
 *   <li><b>Include vectors</b> -- optionally include the stored vectors in query results
 *       (disabled by default for efficiency)</li>
 * </ul>
 *
 * <p>The collection uses a brute-force flat index ({@link FlatIndexParam}) with cosine
 * similarity, which performs an exact search over all vectors. For larger datasets,
 * consider using {@link HnswIndexParam} for approximate nearest neighbor search.</p>
 *
 * <p><b>Run with:</b> {@code ./gradlew :examples:run -PmainClass=io.seqera.zvec.examples.QueryExample}</p>
 */
public class QueryExample {
    public static void main(String[] args) throws Exception {
        // Initialize the zvec native library -- must be called before any other zvec operations
        Zvec.init();

        // Create a temporary directory for the on-disk collection storage
        var tempDir = Files.createTempDirectory("zvec_query");

        // Define the collection schema with three scalar fields and one 4-dimensional
        // vector field. The vector field uses a brute-force flat index with cosine similarity.
        var schema = new CollectionSchema("query_demo",
                List.of(
                        new FieldSchema("title", DataType.STRING),
                        new FieldSchema("category", DataType.STRING),
                        new FieldSchema("score", DataType.FLOAT)
                ),
                List.of(new VectorSchema("emb", DataType.VECTOR_FP32, 4,
                        new FlatIndexParam(MetricType.COSINE))));

        // Create and open the collection. The collection implements AutoCloseable, so
        // the try-with-resources block ensures the handle is released when done.
        try (var coll = Zvec.createAndOpen(tempDir.resolve("query_coll").toString(), schema)) {
            // Insert 20 documents with vectors that point in different directions.
            // Each vector uses sin/cos to rotate through the 4D space, so cosine similarity
            // produces meaningful differences between documents.
            // Documents 1-10 are categorized as "tech", 11-20 as "science".
            for (int i = 1; i <= 20; i++) {
                double angle = i * 0.3;
                coll.insert(new Doc("doc" + i)
                        .field("title", "Document " + i)
                        .field("category", i <= 10 ? "tech" : "science")
                        .field("score", i * 0.5f)
                        .vector("emb", new float[]{
                                (float) Math.cos(angle), (float) Math.sin(angle),
                                (float) Math.cos(angle * 2), (float) Math.sin(angle * 2)}));
            }

            // --- Basic similarity search ---
            // Find the 5 most similar documents to the query vector, ranked by cosine similarity.
            System.out.println("--- Basic Query (top 5) ---");
            // Query vector close to doc5 (angle=1.5): cos(1.5)≈0.07, sin(1.5)≈1.0, cos(3.0)≈-0.99, sin(3.0)≈0.14
            var vq = new VectorQuery("emb", new float[]{0.07f, 1.0f, -0.99f, 0.14f});
            for (var doc : coll.query(vq, 5)) {
                System.out.printf("  id=%s, score=%.4f, title=%s%n", doc.id(), doc.score(), doc.field("title"));
            }

            // --- Filtered query ---
            // Restrict results to documents matching a SQL-like filter expression on scalar fields.
            // Zvec supports SQL-like filter syntax (e.g., equality uses `=`).
            // Only documents in the "tech" category will be considered as candidates.
            System.out.println("\n--- Query with Filter (category = 'tech') ---");
            for (var doc : coll.query(vq, 5, "category = 'tech'", false, null)) {
                System.out.printf("  id=%s, category=%s%n", doc.id(), doc.field("category"));
            }

            // --- Output field selection ---
            // Specify which scalar fields to include in the results. Limiting output fields
            // reduces the amount of data returned, which can improve performance.
            // Here only the "title" field is requested.
            System.out.println("\n--- Query with Output Fields ---");
            for (var doc : coll.query(vq, 3, null, false, new String[]{"title"})) {
                System.out.printf("  id=%s, title=%s%n", doc.id(), doc.field("title"));
            }

            // --- Include vectors in results ---
            // By default, vectors are not included in query results to save memory.
            // Pass includeVector=true to retrieve the stored vectors alongside scalar fields.
            System.out.println("\n--- Query with Include Vector ---");
            for (var doc : coll.query(vq, 2, null, true, null)) {
                System.out.printf("  id=%s, hasVector=%s%n", doc.id(), doc.hasVector("emb"));
            }

            // Destroy the collection to clean up all on-disk data. Note that close() (called
            // automatically by try-with-resources) only releases the native handle; destroy()
            // is needed to actually delete the persisted collection data from disk.
            coll.destroy();
        }
    }
}
