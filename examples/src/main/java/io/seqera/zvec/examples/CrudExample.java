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
 * Demonstrates all CRUD (Create, Read, Update, Delete) operations on a Zvec collection.
 *
 * <p>This example covers the complete document lifecycle:
 * <ol>
 *   <li><b>Insert</b> — add documents individually or in batch</li>
 *   <li><b>Upsert</b> — insert a new document or fully replace an existing one if the ID already exists</li>
 *   <li><b>Update</b> — partially update specific fields of an existing document without replacing the entire document</li>
 *   <li><b>Fetch</b> — retrieve documents by their IDs</li>
 *   <li><b>Delete by ID</b> — remove a single document by its ID</li>
 *   <li><b>Delete by filter</b> — remove documents matching a SQL-like filter expression
 *       (zvec uses {@code =} for equality, not {@code ==})</li>
 * </ol>
 *
 * <p>Run with:
 * <pre>{@code ./gradlew :examples:CrudExample}</pre>
 */
public class CrudExample {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_crud");

        // Define a schema with three scalar fields (title, category, price) and one 4-dimensional
        // vector field using a flat index with cosine similarity.
        var schema = new CollectionSchema("crud",
                List.of(
                        new FieldSchema("title", DataType.STRING),
                        new FieldSchema("category", DataType.STRING),
                        new FieldSchema("price", DataType.FLOAT)
                ),
                List.of(new VectorSchema("emb", DataType.VECTOR_FP32, 4,
                        new FlatIndexParam(MetricType.COSINE))));

        try (var coll = Zvec.createAndOpen(tempDir.resolve("crud_coll").toString(), schema)) {

            // --- INSERT: batch insert multiple documents at once ---
            // The batch insert method accepts a List<Doc> and returns a list of status codes,
            // one per document. Each Doc is built using the fluent builder pattern.
            System.out.println("--- INSERT ---");
            var docs = List.of(
                    new Doc("1").field("title", "Widget A").field("category", "tech").field("price", 9.99f)
                            .vector("emb", new float[]{0.1f, 0.2f, 0.3f, 0.4f}),
                    new Doc("2").field("title", "Widget B").field("category", "tech").field("price", 19.99f)
                            .vector("emb", new float[]{0.5f, 0.6f, 0.7f, 0.8f}),
                    new Doc("3").field("title", "Gadget C").field("category", "science").field("price", 29.99f)
                            .vector("emb", new float[]{0.2f, 0.3f, 0.4f, 0.5f})
            );
            var statuses = coll.insert(docs);
            System.out.println("Inserted: " + statuses);
            System.out.println("Doc count: " + coll.stats().docCount());

            // --- UPSERT: insert-or-replace ---
            // Upsert inserts a new document if the ID does not exist, or fully replaces the
            // existing document if the ID already exists. Here, doc "4" is new and doc "1"
            // already exists so its fields and vector are completely replaced.
            System.out.println("\n--- UPSERT ---");
            coll.upsert(new Doc("4").field("title", "New Item").field("category", "tech").field("price", 5.0f)
                    .vector("emb", new float[]{0.9f, 0.8f, 0.7f, 0.6f}));
            coll.upsert(new Doc("1").field("title", "Widget A Updated").field("category", "tech").field("price", 12.99f)
                    .vector("emb", new float[]{0.15f, 0.25f, 0.35f, 0.45f}));
            System.out.println("Doc count after upsert: " + coll.stats().docCount());

            // --- UPDATE: partial field update ---
            // Unlike upsert, update modifies only the specified fields of an existing document,
            // leaving all other fields and vectors unchanged. Here only the "price" field of doc "2"
            // is changed; its title, category, and vector remain as they were.
            System.out.println("\n--- UPDATE ---");
            coll.update(new Doc("2").field("price", 14.99f));
            System.out.println("Updated doc 2 price");

            // --- FETCH: retrieve documents by ID ---
            // Returns a Map<String, Doc> keyed by document ID. Allows retrieving multiple
            // documents in a single call.
            System.out.println("\n--- FETCH ---");
            var fetched = coll.fetch(List.of("1", "2"));
            for (var entry : fetched.entrySet()) {
                System.out.printf("  %s: title=%s, price=%s%n",
                        entry.getKey(), entry.getValue().field("title"), entry.getValue().field("price"));
            }

            // --- DELETE by ID: remove a single document ---
            System.out.println("\n--- DELETE ---");
            coll.delete("3");
            System.out.println("Doc count after delete: " + coll.stats().docCount());

            // --- DELETE by filter: remove documents matching a SQL-like expression ---
            // Zvec filter syntax uses SQL-like operators: = for equality (not ==),
            // and standard comparison operators (<, >, <=, >=) for numeric fields.
            coll.deleteByFilter("price < 10");
            System.out.println("Doc count after filter delete: " + coll.stats().docCount());

            // Destroy the collection to remove all on-disk data.
            coll.destroy();
        }
    }
}
