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

public enum StatusCode {
    OK(0),
    NOT_FOUND(1),
    ALREADY_EXISTS(2),
    INVALID_ARGUMENT(3),
    PERMISSION_DENIED(4),
    FAILED_PRECONDITION(5),
    RESOURCE_EXHAUSTED(6),
    UNAVAILABLE(7),
    INTERNAL_ERROR(8),
    NOT_SUPPORTED(9),
    UNKNOWN(10);

    private final int value;

    StatusCode(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static StatusCode fromValue(int value) {
        for (StatusCode sc : values()) {
            if (sc.value == value) return sc;
        }
        throw new IllegalArgumentException("Unknown StatusCode value: " + value);
    }
}
