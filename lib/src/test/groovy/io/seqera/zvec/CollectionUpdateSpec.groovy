package io.seqera.zvec

import io.seqera.zvec.param.*
import io.seqera.zvec.schema.*
import io.seqera.zvec.type.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class CollectionUpdateSpec extends Specification {

    @TempDir
    Path tempDir

    Collection coll

    def setup() {
        def schema = new CollectionSchema('test',
            [new FieldSchema('title', DataType.STRING)],
            [new VectorSchema('emb', DataType.VECTOR_FP32, 4, new FlatIndexParam(MetricType.COSINE))]
        )
        coll = Zvec.createAndOpen(tempDir.resolve('test_update').toString(), schema)
        coll.insert(new Doc('1').field('title', 'original').vector('emb', [1f, 2f, 3f, 4f] as float[]))
    }

    def cleanup() {
        coll?.close()
    }

    def 'should update single doc'() {
        when:
        def status = coll.update(new Doc('1').field('title', 'modified'))

        then:
        status == StatusCode.OK
    }

    def 'should update batch'() {
        given:
        coll.insert(new Doc('2').field('title', 'second').vector('emb', [4f, 3f, 2f, 1f] as float[]))

        when:
        def statuses = coll.update([
            new Doc('1').field('title', 'mod1'),
            new Doc('2').field('title', 'mod2')
        ])

        then:
        statuses.size() == 2
    }
}
