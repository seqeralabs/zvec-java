package io.seqera.zvec

import io.seqera.zvec.param.*
import io.seqera.zvec.schema.*
import io.seqera.zvec.type.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class CollectionLifecycleSpec extends Specification {

    @TempDir
    Path tempDir

    def schema = new CollectionSchema('test',
        [new FieldSchema('title', DataType.STRING)],
        [new VectorSchema('emb', DataType.VECTOR_FP32, 4, new FlatIndexParam(MetricType.COSINE))]
    )

    def 'should create and open collection'() {
        when:
        def coll = Zvec.createAndOpen(tempDir.resolve('coll1').toString(), schema)

        then:
        coll != null
        coll.path().contains('coll1')
        coll.schema().name() == 'test'
        coll.schema().fields().size() == 1
        coll.schema().vectors().size() == 1
        coll.stats().docCount() == 0

        cleanup:
        coll?.close()
    }

    def 'should open existing collection'() {
        given:
        def path = tempDir.resolve('coll2').toString()
        def coll1 = Zvec.createAndOpen(path, schema)
        coll1.close()

        when:
        def coll2 = Zvec.open(path)

        then:
        coll2.schema().name() == 'test'

        cleanup:
        coll2?.close()
    }

    def 'should destroy collection'() {
        given:
        def path = tempDir.resolve('coll3').toString()
        def coll = Zvec.createAndOpen(path, schema)

        when:
        coll.destroy()

        then:
        !new File(path).exists() || new File(path).list().length == 0
    }

    def 'should flush collection'() {
        given:
        def coll = Zvec.createAndOpen(tempDir.resolve('coll4').toString(), schema)

        when:
        coll.flush()

        then:
        noExceptionThrown()

        cleanup:
        coll?.close()
    }
}
