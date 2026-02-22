package io.seqera.zvec.getstarted;

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

public class Main {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_getstarted");
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

            // Query most similar documents
            var query = new VectorQuery("embedding", new float[]{0.15f, 0.25f, 0.35f, 0.45f});
            var results = collection.query(query, 2);

            for (var doc : results) {
                System.out.printf("id=%s, score=%.4f, title=%s%n",
                        doc.id(), doc.score(), doc.field("title"));
            }

            // Clean up
            collection.destroy();
        }
    }
}
