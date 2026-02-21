package io.seqera.zvec

import io.seqera.zvec.param.*
import io.seqera.zvec.schema.*
import io.seqera.zvec.type.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class CollectionColumnSpec extends Specification {

    @TempDir
    Path tempDir

    Collection coll

    def setup() {
        def schema = new CollectionSchema('test',
            [new FieldSchema('title', DataType.STRING),
             new FieldSchema('score', DataType.FLOAT)],
            [new VectorSchema('emb', DataType.VECTOR_FP32, 4, new FlatIndexParam(MetricType.COSINE))]
        )
        coll = Zvec.createAndOpen(tempDir.resolve('test_col').toString(), schema)
    }

    def cleanup() {
        coll?.close()
    }

    def 'should add column'() {
        when:
        coll.addColumn(new FieldSchema('priority', DataType.INT32, true), '')

        then:
        coll.schema().field('priority') != null
    }

    def 'should drop column'() {
        when:
        coll.dropColumn('score')

        then:
        coll.schema().field('score') == null
    }

    def 'should alter column rename'() {
        when:
        coll.alterColumn('score', 'rating')

        then:
        coll.schema().field('rating') != null
        coll.schema().field('score') == null
    }
}
