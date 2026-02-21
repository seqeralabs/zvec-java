package io.seqera.zvec

import io.seqera.zvec.param.*
import io.seqera.zvec.schema.*
import io.seqera.zvec.type.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class CollectionFetchSpec extends Specification {

    @TempDir
    Path tempDir

    Collection coll

    def setup() {
        def schema = new CollectionSchema('test',
            [new FieldSchema('title', DataType.STRING)],
            [new VectorSchema('emb', DataType.VECTOR_FP32, 4, new FlatIndexParam(MetricType.COSINE))]
        )
        coll = Zvec.createAndOpen(tempDir.resolve('test_fetch').toString(), schema)
        (1..3).each { i ->
            coll.insert(new Doc("doc$i")
                .field('title', "title $i")
                .vector('emb', [i * 0.1f, i * 0.2f, i * 0.3f, i * 0.4f] as float[]))
        }
    }

    def cleanup() {
        coll?.close()
    }

    def 'should fetch single doc'() {
        when:
        def results = coll.fetch('doc1')

        then:
        results.size() == 1
        results.containsKey('doc1')
        results['doc1'].id() == 'doc1'
        results['doc1'].field('title') == 'title 1'
    }

    def 'should fetch batch'() {
        when:
        def results = coll.fetch(['doc1', 'doc2', 'doc3'])

        then:
        results.size() == 3
    }

    def 'should omit missing ids'() {
        when:
        def results = coll.fetch(['doc1', 'missing'])

        then:
        results.size() == 1
        results.containsKey('doc1')
        !results.containsKey('missing')
    }
}
