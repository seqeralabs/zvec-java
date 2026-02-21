package io.seqera.zvec.examples;

import io.seqera.zvec.*;
import io.seqera.zvec.param.*;
import io.seqera.zvec.schema.*;
import io.seqera.zvec.type.*;

import java.nio.file.Files;
import java.util.List;

public class BasicExample {
    public static void main(String[] args) throws Exception {
        Zvec.init(LogLevel.INFO, 0, 0);

        var tempDir = Files.createTempDirectory("zvec_basic");
        var path = tempDir.resolve("my_collection").toString();

        // Define schema
        var schema = new CollectionSchema("my_collection",
                List.of(new FieldSchema("title", DataType.STRING)),
                List.of(new VectorSchema("embedding", DataType.VECTOR_FP32, 4,
                        new FlatIndexParam(MetricType.COSINE))));

        // Create and open collection
        try (var collection = Zvec.createAndOpen(path, schema)) {
            // Insert documents
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

            // Query
            var query = new VectorQuery("embedding", new float[]{0.15f, 0.25f, 0.35f, 0.45f});
            var results = collection.query(query, 2);

            System.out.println("\nQuery results:");
            for (var doc : results) {
                System.out.printf("  id=%s, score=%.4f, title=%s%n",
                        doc.id(), doc.score(), doc.field("title"));
            }

            // Clean up
            collection.destroy();
        }
    }
}
