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

public class CrudExample {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_crud");
        var schema = new CollectionSchema("crud",
                List.of(
                        new FieldSchema("title", DataType.STRING),
                        new FieldSchema("category", DataType.STRING),
                        new FieldSchema("price", DataType.FLOAT)
                ),
                List.of(new VectorSchema("emb", DataType.VECTOR_FP32, 4,
                        new FlatIndexParam(MetricType.COSINE))));

        try (var coll = Zvec.createAndOpen(tempDir.resolve("crud_coll").toString(), schema)) {

            // Insert
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

            // Upsert
            System.out.println("\n--- UPSERT ---");
            coll.upsert(new Doc("4").field("title", "New Item").field("category", "tech").field("price", 5.0f)
                    .vector("emb", new float[]{0.9f, 0.8f, 0.7f, 0.6f}));
            coll.upsert(new Doc("1").field("title", "Widget A Updated").field("category", "tech").field("price", 12.99f)
                    .vector("emb", new float[]{0.15f, 0.25f, 0.35f, 0.45f}));
            System.out.println("Doc count after upsert: " + coll.stats().docCount());

            // Update
            System.out.println("\n--- UPDATE ---");
            coll.update(new Doc("2").field("price", 14.99f));
            System.out.println("Updated doc 2 price");

            // Fetch
            System.out.println("\n--- FETCH ---");
            var fetched = coll.fetch(List.of("1", "2"));
            for (var entry : fetched.entrySet()) {
                System.out.printf("  %s: title=%s, price=%s%n",
                        entry.getKey(), entry.getValue().field("title"), entry.getValue().field("price"));
            }

            // Delete
            System.out.println("\n--- DELETE ---");
            coll.delete("3");
            System.out.println("Doc count after delete: " + coll.stats().docCount());

            // Delete by filter
            coll.deleteByFilter("price < 10");
            System.out.println("Doc count after filter delete: " + coll.stats().docCount());

            coll.destroy();
        }
    }
}
