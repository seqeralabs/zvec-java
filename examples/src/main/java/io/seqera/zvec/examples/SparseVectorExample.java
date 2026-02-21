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

public class SparseVectorExample {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_sparse");
        var schema = new CollectionSchema("sparse_demo",
                List.of(new FieldSchema("title", DataType.STRING)),
                List.of(new VectorSchema("sparse_emb", DataType.SPARSE_VECTOR_FP32, 0,
                        new FlatIndexParam(MetricType.IP))));

        try (var coll = Zvec.createAndOpen(tempDir.resolve("sparse_coll").toString(), schema)) {
            // Insert documents with sparse vectors
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

            // Query with sparse vector
            var query = new VectorQuery("sparse_emb", new int[]{0, 5, 10}, new float[]{0.15f, 0.45f, 0.9f});
            var results = coll.query(query, 3);

            System.out.println("\nSparse vector query results:");
            for (var doc : results) {
                System.out.printf("  id=%s, score=%.4f, title=%s%n",
                        doc.id(), doc.score(), doc.field("title"));
            }

            coll.destroy();
        }
    }
}
