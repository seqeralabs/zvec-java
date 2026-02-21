/*
 * Copyright 2026, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.seqera.zvec

import io.seqera.zvec.type.DataType
import spock.lang.Specification

class DataTypeSpec extends Specification {

    def 'should have correct enum values'() {
        expect:
        DataType.STRING.value() == 2
        DataType.BOOL.value() == 3
        DataType.INT32.value() == 4
        DataType.INT64.value() == 5
        DataType.UINT32.value() == 6
        DataType.UINT64.value() == 7
        DataType.FLOAT.value() == 8
        DataType.DOUBLE.value() == 9
        DataType.VECTOR_FP32.value() == 23
        DataType.SPARSE_VECTOR_FP32.value() == 31
        DataType.ARRAY_STRING.value() == 41
    }

    def 'should convert from int value'() {
        expect:
        DataType.fromValue(2) == DataType.STRING
        DataType.fromValue(23) == DataType.VECTOR_FP32
        DataType.fromValue(31) == DataType.SPARSE_VECTOR_FP32
    }

    def 'should throw on unknown value'() {
        when:
        DataType.fromValue(999)

        then:
        thrown(IllegalArgumentException)
    }

    def 'should detect vector types'() {
        expect:
        DataType.VECTOR_FP32.isVectorType()
        DataType.VECTOR_FP16.isDenseVectorType()
        DataType.SPARSE_VECTOR_FP32.isSparseVectorType()
        !DataType.STRING.isVectorType()
    }

    def 'should detect scalar types'() {
        expect:
        DataType.STRING.isScalarType()
        DataType.INT32.isScalarType()
        !DataType.VECTOR_FP32.isScalarType()
    }

    def 'should detect array types'() {
        expect:
        DataType.ARRAY_STRING.isArrayType()
        DataType.ARRAY_INT32.isArrayType()
        !DataType.STRING.isArrayType()
    }
}
