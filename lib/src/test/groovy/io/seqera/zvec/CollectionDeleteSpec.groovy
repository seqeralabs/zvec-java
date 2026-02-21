package io.seqera.zvec

import io.seqera.zvec.param.*
import io.seqera.zvec.schema.*
import io.seqera.zvec.type.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class CollectionDeleteSpec extends Specification {

    @TempDir
    Path tempDir

    Collection coll

    def setup() {
        def schema = new CollectionSchema('test',
            [new FieldSchema('title', DataType.STRING),
             new FieldSchema('age', DataType.INT32)],
            [new VectorSchema('emb', DataType.VECTOR_FP32, 4, new FlatIndexParam(MetricType.COSINE))]
        )
        coll = Zvec.createAndOpen(tempDir.resolve('test_delete').toString(), schema)
        (1..5).each { i ->
            coll.insert(new Doc("doc$i")
                .field('title', "title $i")
                .field('age', i * 10)
                .vector('emb', [i * 0.1f, i * 0.2f, i * 0.3f, i * 0.4f] as float[]))
        }
    }

    def cleanup() {
        coll?.close()
    }

    def 'should delete by id'() {
        when:
        def status = coll.delete('doc1')

        then:
        status == StatusCode.OK
        coll.stats().docCount() == 4
    }

    def 'should delete batch'() {
        when:
        def statuses = coll.delete(['doc1', 'doc2', 'doc3'])

        then:
        statuses.size() == 3
        coll.stats().docCount() == 2
    }

    def 'should delete by filter'() {
        when:
        coll.deleteByFilter('age > 30')

        then:
        coll.stats().docCount() == 3
    }
}
