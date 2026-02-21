package io.seqera.zvec

import io.seqera.zvec.param.*
import io.seqera.zvec.schema.*
import io.seqera.zvec.type.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class CollectionQuerySpec extends Specification {

    @TempDir
    Path tempDir

    Collection coll

    def setup() {
        def schema = new CollectionSchema('test',
            [new FieldSchema('title', DataType.STRING),
             new FieldSchema('category', DataType.STRING)],
            [new VectorSchema('emb', DataType.VECTOR_FP32, 4, new FlatIndexParam(MetricType.COSINE))]
        )
        coll = Zvec.createAndOpen(tempDir.resolve('test_query').toString(), schema)
        (1..10).each { i ->
            coll.insert(new Doc("doc$i")
                .field('title', "title $i")
                .field('category', i <= 5 ? 'tech' : 'science')
                .vector('emb', [i * 0.1f, i * 0.2f, i * 0.3f, i * 0.4f] as float[]))
        }
    }

    def cleanup() {
        coll?.close()
    }

    def 'should query basic'() {
        when:
        def vq = new VectorQuery('emb', [0.1f, 0.2f, 0.3f, 0.4f] as float[])
        def results = coll.query(vq, 5)

        then:
        results.size() == 5
        results.every { it.id() != null }
        results.every { it.score() != null }
    }

    def 'should query with filter'() {
        when:
        def vq = new VectorQuery('emb', [0.1f, 0.2f, 0.3f, 0.4f] as float[])
        def results = coll.query(vq, 10, "category = 'tech'", false, null)

        then:
        results.size() == 5
    }

    def 'should query with topk'() {
        when:
        def vq = new VectorQuery('emb', [0.1f, 0.2f, 0.3f, 0.4f] as float[])
        def results = coll.query(vq, 3)

        then:
        results.size() == 3
    }

    def 'should query with output fields'() {
        when:
        def vq = new VectorQuery('emb', [0.1f, 0.2f, 0.3f, 0.4f] as float[])
        def results = coll.query(vq, 5, null, false, ['title'] as String[])

        then:
        results.size() == 5
        results.every { it.hasField('title') }
    }

    def 'should query with include vector'() {
        when:
        def vq = new VectorQuery('emb', [0.1f, 0.2f, 0.3f, 0.4f] as float[])
        def results = coll.query(vq, 3, null, true, null)

        then:
        results.size() == 3
        results.every { it.hasVector('emb') }
    }
}
