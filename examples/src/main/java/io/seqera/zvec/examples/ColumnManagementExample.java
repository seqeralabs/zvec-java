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
 * Demonstrates dynamic schema evolution by adding, renaming, and dropping columns on a live collection.
 *
 * <p>Zvec supports modifying a collection's schema after creation, enabling schema evolution
 * without rebuilding the collection. This example shows three column management operations:
 * <ol>
 *   <li><b>addColumn</b> — add a new scalar field to the schema with a default value for existing documents</li>
 *   <li><b>alterColumn</b> — rename an existing column (the data is preserved under the new name)</li>
 *   <li><b>dropColumn</b> — remove a column from the schema and all documents</li>
 * </ol>
 *
 * <p><b>Important:</b> {@code addColumn} only supports numeric data types (int32, int64, uint32,
 * uint64, float, double). String and other non-numeric types cannot be added dynamically.
 *
 * <p>Run with:
 * <pre>{@code ./gradlew :examples:ColumnManagementExample}</pre>
 */
public class ColumnManagementExample {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_column");

        // Define an initial schema with two scalar fields (title, price) and one vector field.
        var schema = new CollectionSchema("column_demo",
                List.of(
                        new FieldSchema("title", DataType.STRING),
                        new FieldSchema("price", DataType.FLOAT)
                ),
                List.of(new VectorSchema("emb", DataType.VECTOR_FP32, 4,
                        new FlatIndexParam(MetricType.COSINE))));

        try (var coll = Zvec.createAndOpen(tempDir.resolve("column_coll").toString(), schema)) {
            // Insert a document so we can observe the effect of schema changes on existing data.
            coll.insert(new Doc("1").field("title", "Item A").field("price", 10.0f)
                    .vector("emb", new float[]{0.1f, 0.2f, 0.3f, 0.4f}));

            // Add a new nullable FLOAT column "rating" to the schema. The second parameter is the
            // default value assigned to existing documents that do not have this field.
            // Note: addColumn only supports numeric types (int32, int64, uint32, uint64, float, double).
            System.out.println("Adding 'rating' column...");
            coll.addColumn(new FieldSchema("rating", DataType.FLOAT, true), "0.0");
            System.out.println("Schema fields: " + coll.schema().fields().stream()
                    .map(FieldSchema::name).toList());

            // Rename the "price" column to "cost". The column data is preserved; only the name changes.
            System.out.println("Renaming 'price' to 'cost'...");
            coll.alterColumn("price", "cost");
            System.out.println("Schema fields: " + coll.schema().fields().stream()
                    .map(FieldSchema::name).toList());

            // Drop the "rating" column entirely, removing it from the schema and all documents.
            System.out.println("Dropping 'rating' column...");
            coll.dropColumn("rating");
            System.out.println("Schema fields: " + coll.schema().fields().stream()
                    .map(FieldSchema::name).toList());

            // Destroy the collection to clean up all on-disk data.
            coll.destroy();
        }
    }
}
