package io.seqera.zvec

import io.seqera.zvec.type.*
import spock.lang.Specification

class EnumSpec extends Specification {

    def 'IndexType should have correct values'() {
        expect:
        IndexType.UNDEFINED.value() == 0
        IndexType.HNSW.value() == 1
        IndexType.IVF.value() == 3
        IndexType.FLAT.value() == 4
        IndexType.INVERT.value() == 10
        IndexType.fromValue(1) == IndexType.HNSW
    }

    def 'MetricType should have correct values'() {
        expect:
        MetricType.L2.value() == 1
        MetricType.IP.value() == 2
        MetricType.COSINE.value() == 3
        MetricType.fromValue(3) == MetricType.COSINE
    }

    def 'QuantizeType should have correct values'() {
        expect:
        QuantizeType.UNDEFINED.value() == 0
        QuantizeType.FP16.value() == 1
        QuantizeType.INT8.value() == 2
        QuantizeType.INT4.value() == 3
        QuantizeType.fromValue(2) == QuantizeType.INT8
    }

    def 'StatusCode should have correct values'() {
        expect:
        StatusCode.OK.value() == 0
        StatusCode.NOT_FOUND.value() == 1
        StatusCode.ALREADY_EXISTS.value() == 2
        StatusCode.INVALID_ARGUMENT.value() == 3
        StatusCode.INTERNAL_ERROR.value() == 8
        StatusCode.fromValue(0) == StatusCode.OK
    }

    def 'LogLevel should have correct values'() {
        expect:
        LogLevel.DEBUG.value() == 0
        LogLevel.INFO.value() == 1
        LogLevel.WARN.value() == 2
        LogLevel.ERROR.value() == 3
        LogLevel.FATAL.value() == 4
        LogLevel.fromValue(1) == LogLevel.INFO
    }

    def 'fromValue should throw for unknown values'() {
        when:
        IndexType.fromValue(999)
        then:
        thrown(IllegalArgumentException)

        when:
        MetricType.fromValue(999)
        then:
        thrown(IllegalArgumentException)

        when:
        QuantizeType.fromValue(999)
        then:
        thrown(IllegalArgumentException)

        when:
        StatusCode.fromValue(999)
        then:
        thrown(IllegalArgumentException)

        when:
        LogLevel.fromValue(999)
        then:
        thrown(IllegalArgumentException)
    }
}
