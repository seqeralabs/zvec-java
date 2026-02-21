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
import spock.lang.TempDir

import java.nio.file.Path

class CollectionIndexSpec extends Specification {

    @TempDir
    Path tempDir

    Collection coll

    def setup() {
        def schema = new CollectionSchema('test',
            [new FieldSchema('title', DataType.STRING),
             new FieldSchema('score', DataType.FLOAT)],
            [new VectorSchema('emb', DataType.VECTOR_FP32, 4, new FlatIndexParam(MetricType.COSINE))]
        )
        coll = Zvec.createAndOpen(tempDir.resolve('test_index').toString(), schema)
    }

    def cleanup() {
        coll?.close()
    }

    def 'should create and drop HNSW index'() {
        when:
        coll.createIndex('emb', new HnswIndexParam(MetricType.COSINE))

        then:
        noExceptionThrown()

        when:
        coll.dropIndex('emb')

        then:
        noExceptionThrown()
    }

    def 'should create invert index on scalar field'() {
        when:
        coll.createIndex('title', new InvertIndexParam())

        then:
        noExceptionThrown()
    }

    def 'should optimize collection'() {
        given:
        (1..10).each { i ->
            coll.insert(new Doc("doc$i")
                .field('title', "t$i")
                .field('score', (i * 0.1f) as float)
                .vector('emb', [i * 0.1f, i * 0.2f, i * 0.3f, i * 0.4f] as float[]))
        }

        when:
        coll.optimize()

        then:
        noExceptionThrown()
    }
}
