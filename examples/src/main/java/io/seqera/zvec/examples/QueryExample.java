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

public class QueryExample {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_query");
        var schema = new CollectionSchema("query_demo",
                List.of(
                        new FieldSchema("title", DataType.STRING),
                        new FieldSchema("category", DataType.STRING),
                        new FieldSchema("score", DataType.FLOAT)
                ),
                List.of(new VectorSchema("emb", DataType.VECTOR_FP32, 4,
                        new FlatIndexParam(MetricType.COSINE))));

        try (var coll = Zvec.createAndOpen(tempDir.resolve("query_coll").toString(), schema)) {
            // Insert data
            for (int i = 1; i <= 20; i++) {
                coll.insert(new Doc("doc" + i)
                        .field("title", "Document " + i)
                        .field("category", i <= 10 ? "tech" : "science")
                        .field("score", i * 0.5f)
                        .vector("emb", new float[]{i * 0.05f, i * 0.1f, i * 0.15f, i * 0.2f}));
            }

            // Basic query
            System.out.println("--- Basic Query (top 5) ---");
            var vq = new VectorQuery("emb", new float[]{0.1f, 0.2f, 0.3f, 0.4f});
            for (var doc : coll.query(vq, 5)) {
                System.out.printf("  id=%s, score=%.4f, title=%s%n",
                        doc.id(), doc.score(), doc.field("title"));
            }

            // Query with filter
            System.out.println("\n--- Query with Filter (category == 'tech') ---");
            for (var doc : coll.query(vq, 5, "category == 'tech'", false, null)) {
                System.out.printf("  id=%s, category=%s%n", doc.id(), doc.field("category"));
            }

            // Query with output fields
            System.out.println("\n--- Query with Output Fields ---");
            for (var doc : coll.query(vq, 3, null, false, new String[]{"title"})) {
                System.out.printf("  id=%s, title=%s%n", doc.id(), doc.field("title"));
            }

            // Query with include vector
            System.out.println("\n--- Query with Include Vector ---");
            for (var doc : coll.query(vq, 2, null, true, null)) {
                System.out.printf("  id=%s, hasVector=%s%n", doc.id(), doc.hasVector("emb"));
            }

            coll.destroy();
        }
    }
}
