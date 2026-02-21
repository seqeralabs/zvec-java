package io.seqera.zvec

import io.seqera.zvec.param.*
import io.seqera.zvec.type.*
import spock.lang.Specification

class ParamSpec extends Specification {

    def 'HnswIndexParam defaults'() {
        when:
        def p = new HnswIndexParam(MetricType.COSINE)

        then:
        p.metricType() == MetricType.COSINE
        p.m() == 50
        p.efConstruction() == 500
        p.quantizeType() == QuantizeType.UNDEFINED
    }

    def 'HnswIndexParam custom values'() {
        when:
        def p = new HnswIndexParam(MetricType.L2, 32, 200, QuantizeType.FP16)

        then:
        p.metricType() == MetricType.L2
        p.m() == 32
        p.efConstruction() == 200
        p.quantizeType() == QuantizeType.FP16
    }

    def 'FlatIndexParam defaults'() {
        when:
        def p = new FlatIndexParam(MetricType.IP)

        then:
        p.metricType() == MetricType.IP
        p.quantizeType() == QuantizeType.UNDEFINED
    }

    def 'IVFIndexParam defaults'() {
        when:
        def p = new IVFIndexParam(MetricType.COSINE)

        then:
        p.metricType() == MetricType.COSINE
        p.nList() == 1024
        p.nIters() == 10
        !p.useSoar()
        p.quantizeType() == QuantizeType.UNDEFINED
    }

    def 'IVFIndexParam custom values'() {
        when:
        def p = new IVFIndexParam(MetricType.L2, 512, 20, true, QuantizeType.INT8)

        then:
        p.nList() == 512
        p.nIters() == 20
        p.useSoar()
        p.quantizeType() == QuantizeType.INT8
    }

    def 'InvertIndexParam defaults'() {
        when:
        def p = new InvertIndexParam()

        then:
        p.enableRangeOptimization()
        !p.enableExtendedWildcard()
    }

    def 'HnswQueryParam defaults'() {
        when:
        def p = new HnswQueryParam()

        then:
        p.ef() == 300
        p.radius() == 0.0f
        !p.isLinear()
        !p.isUsingRefiner()
    }

    def 'IVFQueryParam defaults'() {
        when:
        def p = new IVFQueryParam()

        then:
        p.nprobe() == 10
        !p.isUsingRefiner()
        p.scaleFactor() == 10.0f
    }

    def 'FlatQueryParam defaults'() {
        when:
        def p = new FlatQueryParam()

        then:
        !p.isUsingRefiner()
        p.scaleFactor() == 10.0f
    }

    def 'CollectionOption defaults'() {
        when:
        def opt = new CollectionOption()

        then:
        !opt.readOnly()
        opt.enableMmap()
        opt.maxBufferSize() == 64 * 1024 * 1024
    }

    def 'CollectionOption fluent builder'() {
        when:
        def opt = new CollectionOption().readOnly(true).enableMmap(false).maxBufferSize(1024)

        then:
        opt.readOnly()
        !opt.enableMmap()
        opt.maxBufferSize() == 1024
    }

    def 'VectorQuery by vector'() {
        when:
        def vq = new VectorQuery('emb', [0.1f, 0.2f, 0.3f] as float[])

        then:
        vq.fieldName() == 'emb'
        vq.vector().length == 3
        vq.sparseIndices() == null
    }

    def 'VectorQuery sparse'() {
        when:
        def vq = new VectorQuery('sparse', [0, 5, 10] as int[], [0.1f, 0.5f, 1.0f] as float[])

        then:
        vq.fieldName() == 'sparse'
        vq.sparseIndices().length == 3
        vq.sparseValues().length == 3
        vq.vector() == null
    }

    def 'VectorQuery with query param'() {
        when:
        def vq = new VectorQuery('emb', [1.0f] as float[]).queryParam(new HnswQueryParam(500))

        then:
        vq.queryParam() instanceof HnswQueryParam
        ((HnswQueryParam) vq.queryParam()).ef() == 500
    }
}
