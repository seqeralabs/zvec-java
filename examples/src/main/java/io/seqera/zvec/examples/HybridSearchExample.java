package io.seqera.zvec.examples;

import io.seqera.zvec.*;
import io.seqera.zvec.param.*;
import io.seqera.zvec.schema.*;
import io.seqera.zvec.type.*;

import java.nio.file.Files;
import java.util.List;

public class HybridSearchExample {
    public static void main(String[] args) throws Exception {
        Zvec.init();

        var tempDir = Files.createTempDirectory("zvec_hybrid");

        // Schema with both dense and sparse vectors
        var schema = new CollectionSchema("hybrid_demo",
                List.of(new FieldSchema("title", DataType.STRING)),
                List.of(
                        new VectorSchema("dense_emb", DataType.VECTOR_FP32, 4,
                                new FlatIndexParam(MetricType.COSINE)),
                        new VectorSchema("sparse_emb", DataType.SPARSE_VECTOR_FP32, 0,
                                new FlatIndexParam(MetricType.IP))
                ));

        try (var coll = Zvec.createAndOpen(tempDir.resolve("hybrid_coll").toString(), schema)) {
            // Insert documents with both dense and sparse vectors
            coll.insert(new Doc("1")
                    .field("title", "Vector Database Introduction")
                    .vector("dense_emb", new float[]{0.1f, 0.2f, 0.3f, 0.4f})
                    .sparseVector("sparse_emb", new int[]{0, 5, 10}, new float[]{0.1f, 0.5f, 1.0f}));
            coll.insert(new Doc("2")
                    .field("title", "Machine Learning Guide")
                    .vector("dense_emb", new float[]{0.5f, 0.6f, 0.7f, 0.8f})
                    .sparseVector("sparse_emb", new int[]{1, 3, 7}, new float[]{0.3f, 0.7f, 0.2f}));
            coll.insert(new Doc("3")
                    .field("title", "Deep Learning Fundamentals")
                    .vector("dense_emb", new float[]{0.2f, 0.3f, 0.4f, 0.5f})
                    .sparseVector("sparse_emb", new int[]{0, 5, 10, 15}, new float[]{0.2f, 0.4f, 0.8f, 0.1f}));

            System.out.println("Inserted " + coll.stats().docCount() + " hybrid documents");

            // Dense vector query
            System.out.println("\n--- Dense Vector Query ---");
            var denseQuery = new VectorQuery("dense_emb", new float[]{0.15f, 0.25f, 0.35f, 0.45f});
            for (var doc : coll.query(denseQuery, 3)) {
                System.out.printf("  id=%s, score=%.4f, title=%s%n",
                        doc.id(), doc.score(), doc.field("title"));
            }

            // Sparse vector query
            System.out.println("\n--- Sparse Vector Query ---");
            var sparseQuery = new VectorQuery("sparse_emb",
                    new int[]{0, 5, 10}, new float[]{0.15f, 0.45f, 0.9f});
            for (var doc : coll.query(sparseQuery, 3)) {
                System.out.printf("  id=%s, score=%.4f, title=%s%n",
                        doc.id(), doc.score(), doc.field("title"));
            }

            coll.destroy();
        }
    }
}
