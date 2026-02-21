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

package io.seqera.zvec.type;

public enum IndexType {
    UNDEFINED(0),
    HNSW(1),
    IVF(3),
    FLAT(4),
    INVERT(10);

    private final int value;

    IndexType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static IndexType fromValue(int value) {
        for (IndexType t : values()) {
            if (t.value == value) return t;
        }
        throw new IllegalArgumentException("Unknown IndexType value: " + value);
    }
}
