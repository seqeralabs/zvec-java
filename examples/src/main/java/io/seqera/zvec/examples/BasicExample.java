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
import io.seqera.zvec.param.VectorQuery;
import io.seqera.zvec.schema.CollectionSchema;
import io.seqera.zvec.schema.FieldSchema;
import io.seqera.zvec.schema.VectorSchema;
import io.seqera.zvec.type.DataType;
import io.seqera.zvec.type.MetricType;

/**
 * Basic introduction to zvec-java: creating a collection, inserting documents, and querying by vector similarity.
 *
 * <p>This example demonstrates the fundamental workflow for using the Zvec vector database from Java:
 * <ol>
 *   <li>Initialize the Zvec native library</li>
 *   <li>Define a collection schema with scalar fields and a vector field</li>
 *   <li>Create and open a collection on disk</li>
 *   <li>Insert documents using the fluent {@link Doc} builder</li>
 *   <li>Perform a vector similarity query using cosine similarity</li>
 *   <li>Clean up by destroying the collection (removes on-disk data)</li>
 * </ol>
 *
 * <p>Concepts covered:
 * <ul>
 *   <li>{@link CollectionSchema} — defines the structure of a collection (scalar fields + vector fields)</li>
 *   <li>{@link FlatIndexParam} — brute-force flat index that compares the query against every vector (exact search)</li>
 *   <li>{@link MetricType#COSINE} — cosine similarity metric for measuring vector closeness</li>
 *   <li>{@link VectorQuery} — specifies which vector field to search and the query vector</li>
 *   <li>{@link Doc} — fluent builder for constructing documents with scalar fields and vectors</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>{@code ./gradlew :examples:run}</pre>
 */
public class BasicExample {
    public static void main(String[] args) throws Exception {
        // Initialize the Zvec native library. This must be called once before any other Zvec operations.
        Zvec.init();

        // Create a temporary directory to store the collection's on-disk data.
        var tempDir = Files.createTempDirectory("zvec_basic");
        var path = tempDir.resolve("my_collection").toString();

        // Define the collection schema with:
        //  - One scalar field "title" of type STRING for storing document metadata
        //  - One vector field "embedding" of type VECTOR_FP32 with 4 dimensions,
        //    using a brute-force flat index with cosine similarity as the distance metric
        var schema = new CollectionSchema("my_collection",
                List.of(new FieldSchema("title", DataType.STRING)),
                List.of(new VectorSchema("embedding", DataType.VECTOR_FP32, 4,
                        new FlatIndexParam(MetricType.COSINE))));

        // Create a new collection on disk and open it. The collection implements AutoCloseable,
        // so the try-with-resources block will release the native handle when done.
        try (var collection = Zvec.createAndOpen(path, schema)) {
            // Insert three documents using the fluent Doc builder. Each document has a unique ID,
            // a "title" scalar field, and an "embedding" vector with 4 float components.
            collection.insert(new Doc("1")
                    .field("title", "Introduction to Vector Databases")
                    .vector("embedding", new float[]{0.1f, 0.2f, 0.3f, 0.4f}));
            collection.insert(new Doc("2")
                    .field("title", "Machine Learning Basics")
                    .vector("embedding", new float[]{0.5f, 0.6f, 0.7f, 0.8f}));
            collection.insert(new Doc("3")
                    .field("title", "Deep Learning with Neural Networks")
                    .vector("embedding", new float[]{0.2f, 0.3f, 0.4f, 0.5f}));

            System.out.println("Inserted " + collection.stats().docCount() + " documents");

            // Perform a vector similarity query: find the 2 documents whose "embedding" vectors
            // are closest to the given query vector, ranked by cosine similarity score.
            var query = new VectorQuery("embedding", new float[]{0.15f, 0.25f, 0.35f, 0.45f});
            var results = collection.query(query, 2);

            // Print the results. Each result includes the document ID, similarity score, and fields.
            System.out.println("\nQuery results:");
            for (var doc : results) {
                System.out.printf("  id=%s, score=%.4f, title=%s%n",
                        doc.id(), doc.score(), doc.field("title"));
            }

            // Destroy the collection to remove all on-disk data. Note: simply closing the
            // collection only releases the native handle but leaves the data on disk.
            collection.destroy();
        }
    }
}
