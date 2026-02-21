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

import spock.lang.Specification

class DocSpec extends Specification {

    def 'should create doc with id'() {
        when:
        def doc = new Doc('doc1')

        then:
        doc.id() == 'doc1'
        doc.score() == null
        doc.fieldNames().empty
        doc.vectorNames().empty
    }

    def 'should set and get fields'() {
        when:
        def doc = new Doc('doc1')
            .field('name', 'hello')
            .field('age', 42)
            .field('score', 0.95f)
            .field('active', true)

        then:
        doc.field('name') == 'hello'
        doc.field('age') == 42
        doc.field('score') == 0.95f
        doc.field('active') == true
        doc.hasField('name')
        !doc.hasField('missing')
    }

    def 'should set and get vectors'() {
        when:
        def doc = new Doc('doc1')
            .vector('emb', [0.1f, 0.2f, 0.3f] as float[])

        then:
        doc.hasVector('emb')
        ((float[]) doc.vector('emb')).length == 3
        !doc.hasVector('missing')
    }

    def 'should support sparse vectors'() {
        when:
        def doc = new Doc('doc1')
            .sparseVector('sparse', [0, 5] as int[], [0.1f, 0.5f] as float[])

        then:
        doc.hasVector('sparse')
        def sv = doc.vector('sparse') as Doc.SparseVector
        sv.indices().length == 2
        sv.values().length == 2
    }

    def 'should handle null fields'() {
        when:
        def doc = new Doc('doc1').nullField('optional')

        then:
        doc.hasField('optional')
        doc.field('optional') == null
    }

    def 'should create with maps'() {
        when:
        def doc = new Doc('doc1', [name: 'test'], [emb: [1.0f, 2.0f] as float[]])

        then:
        doc.field('name') == 'test'
        doc.hasVector('emb')
    }

    def 'should set score'() {
        when:
        def doc = new Doc('doc1')
        doc.setScore(0.99f)

        then:
        doc.score() == 0.99f
    }
}
