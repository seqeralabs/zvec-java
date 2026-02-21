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

import io.seqera.zvec.param.*
import io.seqera.zvec.schema.*
import io.seqera.zvec.type.*
import spock.lang.Specification

class SchemaSpec extends Specification {

    def 'FieldSchema should store properties'() {
        when:
        def fs = new FieldSchema('name', DataType.STRING)

        then:
        fs.name() == 'name'
        fs.dataType() == DataType.STRING
        !fs.nullable()
        fs.indexParam() == null
    }

    def 'FieldSchema should support nullable'() {
        when:
        def fs = new FieldSchema('age', DataType.INT32, true)

        then:
        fs.nullable()
    }

    def 'FieldSchema should support index params'() {
        when:
        def ip = new InvertIndexParam()
        def fs = new FieldSchema('tag', DataType.STRING, false, ip)

        then:
        fs.indexParam() != null
        fs.indexParam() instanceof InvertIndexParam
    }

    def 'VectorSchema should store properties'() {
        when:
        def vs = new VectorSchema('emb', DataType.VECTOR_FP32, 128)

        then:
        vs.name() == 'emb'
        vs.dataType() == DataType.VECTOR_FP32
        vs.dimension() == 128
        !vs.nullable()
    }

    def 'VectorSchema with index param'() {
        when:
        def ip = new HnswIndexParam(MetricType.COSINE)
        def vs = new VectorSchema('emb', DataType.VECTOR_FP32, 128, ip)

        then:
        vs.indexParam() != null
    }

    def 'CollectionSchema should hold fields and vectors'() {
        when:
        def schema = new CollectionSchema('test',
            [new FieldSchema('title', DataType.STRING)],
            [new VectorSchema('emb', DataType.VECTOR_FP32, 128)]
        )

        then:
        schema.name() == 'test'
        schema.fields().size() == 1
        schema.vectors().size() == 1
        schema.field('title') != null
        schema.field('title').dataType() == DataType.STRING
        schema.vector('emb') != null
        schema.vector('emb').dimension() == 128
    }

    def 'CollectionSchema field/vector returns null for missing'() {
        when:
        def schema = new CollectionSchema('test', [], [])

        then:
        schema.field('missing') == null
        schema.vector('missing') == null
    }
}
