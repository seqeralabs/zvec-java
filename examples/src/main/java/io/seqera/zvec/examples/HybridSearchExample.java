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
 * Demonstrates hybrid search combining dense and sparse vectors in the same collection.
 *
 * <p>Hybrid search is a common pattern in information retrieval where two complementary
 * vector representations are used together:</p>
 * <ul>
 *   <li><b>Dense vectors</b> -- fixed-dimension float arrays (e.g., from neural embedding
 *       models) that capture semantic similarity. Queried with cosine similarity.</li>
 *   <li><b>Sparse vectors</b> -- index/value pairs representing keyword or feature-based
 *       signals (e.g., TF-IDF or BM25 term weights). Only non-zero dimensions are stored.
 *       Queried with inner product (IP), which is the natural metric for weighted
 *       term-matching scores.</li>
 * </ul>
 *
 * <p>In a real-world application, dense and sparse results can be fused (e.g., using
 * reciprocal rank fusion) to combine semantic understanding with keyword precision.</p>
 *
 * <p>Key details:</p>
 * <ul>
 *   <li>The sparse vector schema uses dimension 0 because sparse vectors have variable
 *       dimensionality -- the actual dimensions are determined by the indices provided
 *       at insert time</li>
 *   <li>Dense vectors use {@link MetricType#COSINE} for semantic similarity</li>
 *   <li>Sparse vectors use {@link MetricType#IP} (inner product), which naturally computes
 *       the dot product of overlapping non-zero dimensions</li>
 *   <li>Sparse vector queries use a {@link VectorQuery} constructed with parallel arrays
 *       of indices (non-zero dimension positions) and values</li>
 * </ul>
 *
 * <p><b>Run with:</b> {@code ./gradlew :examples:run -PmainClass=io.seqera.zvec.examples.HybridSearchExample}</p>
 */
public class HybridSearchExample {
    public static void main(String[] args) throws Exception {
        // Initialize the zvec native library -- must be called before any other zvec operations
        Zvec.init();

        // Create a temporary directory for the on-disk collection storage
        var tempDir = Files.createTempDirectory("zvec_hybrid");

        // Define a schema with both dense and sparse vector fields.
        // - "dense_emb": a 4-dimensional dense float vector using cosine similarity
        // - "sparse_emb": a sparse float vector with dimension 0 (variable dimensionality)
        //   using inner product (IP). Dimension 0 signals to zvec that the vector has
        //   variable dimensionality; actual dimensions are determined by the indices
        //   provided when inserting documents.
        var schema = new CollectionSchema("hybrid_demo",
                List.of(new FieldSchema("title", DataType.STRING)),
                List.of(
                        new VectorSchema("dense_emb", DataType.VECTOR_FP32, 4,
                                new FlatIndexParam(MetricType.COSINE)),
                        new VectorSchema("sparse_emb", DataType.SPARSE_VECTOR_FP32, 0,
                                new FlatIndexParam(MetricType.IP))
                ));

        // Create and open the collection. The collection implements AutoCloseable, so
        // the try-with-resources block ensures the handle is released when done.
        try (var coll = Zvec.createAndOpen(tempDir.resolve("hybrid_coll").toString(), schema)) {
            // Insert documents with both dense and sparse vectors using the fluent Doc builder.
            // Dense vectors are set with .vector() as fixed-size float arrays.
            // Sparse vectors are set with .sparseVector() using parallel arrays:
            //   - int[] indices: the non-zero dimension positions
            //   - float[] values: the corresponding values at those positions
            // For example, {indices=[0,5,10], values=[0.1,0.5,1.0]} means dimension 0 has
            // value 0.1, dimension 5 has value 0.5, and dimension 10 has value 1.0.
            coll.insert(new Doc("1")
                    .field("title", "Vector Database Introduction")
                    .vector("dense_emb", new float[]{0.1f, 0.2f, 0.3f, 0.4f})
                    .sparseVector("sparse_emb", new int[]{0, 5, 10}, new float[]{0.1f, 0.5f, 1.0f}));
            coll.insert(new Doc("2")
                    .field("title", "Machine Learning Guide")
                    .vector("dense_emb", new float[]{0.5f, 0.6f, 0.7f, 0.8f})
                    .sparseVector("sparse_emb", new int[]{1, 3, 7}, new float[]{0.3f, 0.7f, 0.2f}));
            coll.insert(new Doc("3")
                    .field("title", "Deep Learning Fundamentals")
                    .vector("dense_emb", new float[]{0.2f, 0.3f, 0.4f, 0.5f})
                    .sparseVector("sparse_emb", new int[]{0, 5, 10, 15}, new float[]{0.2f, 0.4f, 0.8f, 0.1f}));

            System.out.println("Inserted " + coll.stats().docCount() + " hybrid documents");

            // --- Dense vector query ---
            // Search using the dense embedding field with cosine similarity.
            // This finds documents whose semantic embeddings are closest to the query vector.
            System.out.println("\n--- Dense Vector Query ---");
            var denseQuery = new VectorQuery("dense_emb", new float[]{0.15f, 0.25f, 0.35f, 0.45f});
            for (var doc : coll.query(denseQuery, 3)) {
                System.out.printf("  id=%s, score=%.4f, title=%s%n",
                        doc.id(), doc.score(), doc.field("title"));
            }

            // --- Sparse vector query ---
            // Search using the sparse embedding field with inner product (IP).
            // The VectorQuery for sparse vectors takes the field name, an int[] of
            // non-zero dimension indices, and a float[] of corresponding values.
            // The inner product is computed only over overlapping non-zero dimensions
            // between the query and stored vectors.
            System.out.println("\n--- Sparse Vector Query ---");
            var sparseQuery = new VectorQuery("sparse_emb",
                    new int[]{0, 5, 10}, new float[]{0.15f, 0.45f, 0.9f});
            for (var doc : coll.query(sparseQuery, 3)) {
                System.out.printf("  id=%s, score=%.4f, title=%s%n",
                        doc.id(), doc.score(), doc.field("title"));
            }

            // Destroy the collection to clean up all on-disk data. Note that close() (called
            // automatically by try-with-resources) only releases the native handle; destroy()
            // is needed to actually delete the persisted collection data from disk.
            coll.destroy();
        }
    }
}
