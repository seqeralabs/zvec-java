package io.seqera.zvec

import io.seqera.zvec.type.StatusCode
import spock.lang.Specification

class ZvecExceptionSpec extends Specification {

    def 'should carry status code and message'() {
        when:
        def ex = new ZvecException(StatusCode.NOT_FOUND, 'item not found')

        then:
        ex.statusCode() == StatusCode.NOT_FOUND
        ex.message == 'item not found'
        ex.toString().contains('NOT_FOUND')
    }
}
