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

public class ColumnManagementExample {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_column");
        var schema = new CollectionSchema("column_demo",
                List.of(
                        new FieldSchema("title", DataType.STRING),
                        new FieldSchema("price", DataType.FLOAT)
                ),
                List.of(new VectorSchema("emb", DataType.VECTOR_FP32, 4,
                        new FlatIndexParam(MetricType.COSINE))));

        try (var coll = Zvec.createAndOpen(tempDir.resolve("column_coll").toString(), schema)) {
            // Insert data
            coll.insert(new Doc("1").field("title", "Item A").field("price", 10.0f)
                    .vector("emb", new float[]{0.1f, 0.2f, 0.3f, 0.4f}));

            // Add column
            System.out.println("Adding 'tags' column...");
            coll.addColumn(new FieldSchema("tags", DataType.STRING, true), "");
            System.out.println("Schema fields: " + coll.schema().fields().stream()
                    .map(FieldSchema::name).toList());

            // Alter column (rename)
            System.out.println("Renaming 'price' to 'cost'...");
            coll.alterColumn("price", "cost");
            System.out.println("Schema fields: " + coll.schema().fields().stream()
                    .map(FieldSchema::name).toList());

            // Drop column
            System.out.println("Dropping 'tags' column...");
            coll.dropColumn("tags");
            System.out.println("Schema fields: " + coll.schema().fields().stream()
                    .map(FieldSchema::name).toList());

            coll.destroy();
        }
    }
}
