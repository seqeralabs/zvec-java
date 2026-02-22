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
 * Demonstrates sparse vector support in the zvec vector database.
 *
 * <p>Sparse vectors are an efficient representation for high-dimensional, mostly-zero data
 * such as TF-IDF term weights, BM25 scores, or one-hot encoded features. Instead of
 * storing a full dense array (which would be mostly zeros), sparse vectors store only the
 * non-zero entries as parallel arrays of indices and values:</p>
 * <ul>
 *   <li><b>indices</b> ({@code int[]}) -- the dimension positions that have non-zero values</li>
 *   <li><b>values</b> ({@code float[]}) -- the corresponding values at those positions</li>
 * </ul>
 *
 * <p>Key details:</p>
 * <ul>
 *   <li>The sparse vector schema specifies dimension 0, which signals variable dimensionality
 *       -- the actual dimensions are determined by the indices provided at insert time</li>
 *   <li>Inner product ({@link MetricType#IP}) is the natural metric for sparse vectors,
 *       as it computes the dot product over overlapping non-zero dimensions, matching
 *       how term-weight scoring (e.g., TF-IDF) works</li>
 *   <li>A brute-force flat index ({@link FlatIndexParam}) performs exact search over all
 *       sparse vectors in the collection</li>
 *   <li>The {@link VectorQuery} for sparse vectors is constructed with the field name,
 *       an {@code int[]} of non-zero dimension indices, and a {@code float[]} of values</li>
 * </ul>
 *
 * <p><b>Run with:</b> {@code ./gradlew :examples:run -PmainClass=io.seqera.zvec.examples.SparseVectorExample}</p>
 */
public class SparseVectorExample {
    public static void main(String[] args) throws Exception {
        // Initialize the zvec native library -- must be called before any other zvec operations
        Zvec.init();

        // Create a temporary directory for the on-disk collection storage
        var tempDir = Files.createTempDirectory("zvec_sparse");

        // Define the collection schema with a single scalar field and one sparse vector field.
        // The sparse vector uses SPARSE_VECTOR_FP32 data type with dimension 0, which means
        // variable dimensionality -- each document can have non-zero values at different
        // dimension positions. Inner product (IP) is used as the distance metric, which
        // computes the dot product over overlapping non-zero dimensions.
        var schema = new CollectionSchema("sparse_demo",
                List.of(new FieldSchema("title", DataType.STRING)),
                List.of(new VectorSchema("sparse_emb", DataType.SPARSE_VECTOR_FP32, 0,
                        new FlatIndexParam(MetricType.IP))));

        // Create and open the collection. The collection implements AutoCloseable, so
        // the try-with-resources block ensures the handle is released when done.
        try (var coll = Zvec.createAndOpen(tempDir.resolve("sparse_coll").toString(), schema)) {
            // Insert documents with sparse vectors using the fluent Doc builder.
            // Each sparse vector is specified with .sparseVector(fieldName, indices, values):
            //   - indices (int[]): the non-zero dimension positions
            //   - values (float[]): the corresponding weight at each position
            // For example, doc "1" has non-zero values at dimensions 0, 5, and 10.
            // Note that each document can have different numbers of non-zero entries
            // and different dimension positions.
            coll.insert(new Doc("1")
                    .field("title", "Sparse doc 1")
                    .sparseVector("sparse_emb", new int[]{0, 5, 10}, new float[]{0.1f, 0.5f, 1.0f}));
            coll.insert(new Doc("2")
                    .field("title", "Sparse doc 2")
                    .sparseVector("sparse_emb", new int[]{1, 3, 7}, new float[]{0.3f, 0.7f, 0.2f}));
            coll.insert(new Doc("3")
                    .field("title", "Sparse doc 3")
                    .sparseVector("sparse_emb", new int[]{0, 5, 10, 15}, new float[]{0.2f, 0.4f, 0.8f, 0.1f}));

            System.out.println("Inserted " + coll.stats().docCount() + " sparse vector documents");

            // Query with a sparse vector. The VectorQuery constructor for sparse vectors
            // takes the field name, an int[] of query dimension indices, and a float[] of
            // query values. The inner product score is computed by summing the products of
            // values at overlapping non-zero dimensions between the query and each stored
            // vector. Documents with higher overlap and larger matching values score higher.
            var query = new VectorQuery("sparse_emb", new int[]{0, 5, 10}, new float[]{0.15f, 0.45f, 0.9f});
            var results = coll.query(query, 3);

            System.out.println("\nSparse vector query results:");
            for (var doc : results) {
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
