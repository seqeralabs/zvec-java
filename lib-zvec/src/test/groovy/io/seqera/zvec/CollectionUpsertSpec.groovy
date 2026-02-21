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

class CollectionUpsertSpec extends Specification {

    @TempDir
    Path tempDir

    Collection coll

    def setup() {
        def schema = new CollectionSchema('test',
            [new FieldSchema('title', DataType.STRING)],
            [new VectorSchema('emb', DataType.VECTOR_FP32, 4, new FlatIndexParam(MetricType.COSINE))]
        )
        coll = Zvec.createAndOpen(tempDir.resolve('test_upsert').toString(), schema)
    }

    def cleanup() {
        coll?.close()
    }

    def 'should upsert new doc'() {
        when:
        def status = coll.upsert(new Doc('1').field('title', 'first').vector('emb', [1f, 2f, 3f, 4f] as float[]))

        then:
        status == StatusCode.OK
        coll.stats().docCount() == 1
    }

    def 'should upsert existing doc'() {
        given:
        coll.insert(new Doc('1').field('title', 'first').vector('emb', [1f, 2f, 3f, 4f] as float[]))

        when:
        def status = coll.upsert(new Doc('1').field('title', 'updated').vector('emb', [4f, 3f, 2f, 1f] as float[]))

        then:
        status == StatusCode.OK
        coll.stats().docCount() == 1
    }
}
