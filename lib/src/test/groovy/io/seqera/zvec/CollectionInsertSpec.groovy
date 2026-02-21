package io.seqera.zvec

import io.seqera.zvec.param.*
import io.seqera.zvec.schema.*
import io.seqera.zvec.type.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class CollectionInsertSpec extends Specification {

    @TempDir
    Path tempDir

    Collection coll

    def setup() {
        def schema = new CollectionSchema('test',
            [new FieldSchema('title', DataType.STRING),
             new FieldSchema('score', DataType.FLOAT, true)],
            [new VectorSchema('emb', DataType.VECTOR_FP32, 4, new FlatIndexParam(MetricType.COSINE))]
        )
        coll = Zvec.createAndOpen(tempDir.resolve('test_insert').toString(), schema)
    }

    def cleanup() {
        coll?.close()
    }

    def 'should insert single doc'() {
        when:
        def doc = new Doc('1')
            .field('title', 'hello')
            .vector('emb', [0.1f, 0.2f, 0.3f, 0.4f] as float[])
        def status = coll.insert(doc)

        then:
        status == StatusCode.OK
        coll.stats().docCount() == 1
    }

    def 'should insert batch'() {
        when:
        def docs = (1..5).collect { i ->
            new Doc("doc$i")
                .field('title', "title $i")
                .vector('emb', [i * 0.1f, i * 0.2f, i * 0.3f, i * 0.4f] as float[])
        }
        def statuses = coll.insert(docs)

        then:
        statuses.size() == 5
        statuses.every { it == StatusCode.OK }
        coll.stats().docCount() == 5
    }

    def 'should insert with nullable field'() {
        when:
        def doc = new Doc('n1')
            .field('title', 'test')
            .nullField('score')
            .vector('emb', [1f, 2f, 3f, 4f] as float[])
        def status = coll.insert(doc)

        then:
        status == StatusCode.OK
    }
}
