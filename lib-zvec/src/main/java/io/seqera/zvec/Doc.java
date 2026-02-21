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

package io.seqera.zvec;

import java.util.*;

public class Doc {
    private final String id;
    private Float score;
    private final Map<String, Object> fields;
    private final Map<String, Object> vectors;

    public Doc(String id) {
        this(id, null, null);
    }

    public Doc(String id, Map<String, Object> fields, Map<String, Object> vectors) {
        this.id = id;
        this.fields = fields != null ? new LinkedHashMap<>(fields) : new LinkedHashMap<>();
        this.vectors = vectors != null ? new LinkedHashMap<>(vectors) : new LinkedHashMap<>();
    }

    public String id() { return id; }

    public Float score() { return score; }

    public void setScore(Float score) { this.score = score; }

    public Object field(String name) {
        return fields.get(name);
    }

    public Object vector(String name) {
        return vectors.get(name);
    }

    public Set<String> fieldNames() {
        return Collections.unmodifiableSet(fields.keySet());
    }

    public Set<String> vectorNames() {
        return Collections.unmodifiableSet(vectors.keySet());
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public boolean hasVector(String name) {
        return vectors.containsKey(name);
    }

    public Doc field(String name, Object value) {
        fields.put(name, value);
        return this;
    }

    public Doc vector(String name, float[] value) {
        vectors.put(name, value);
        return this;
    }

    public Doc vector(String name, double[] value) {
        vectors.put(name, value);
        return this;
    }

    public Doc sparseVector(String name, int[] indices, float[] values) {
        vectors.put(name, new SparseVector(indices, values));
        return this;
    }

    public Doc nullField(String name) {
        fields.put(name, null);
        return this;
    }

    public Map<String, Object> fields() { return Collections.unmodifiableMap(fields); }
    public Map<String, Object> vectors() { return Collections.unmodifiableMap(vectors); }

    public record SparseVector(int[] indices, float[] values) {}

    @Override
    public String toString() {
        return "Doc{id='" + id + "', score=" + score + ", fields=" + fields.keySet() + ", vectors=" + vectors.keySet() + "}";
    }
}
