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

public enum LogLevel {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    FATAL(4);

    private final int value;

    LogLevel(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static LogLevel fromValue(int value) {
        for (LogLevel ll : values()) {
            if (ll.value == value) return ll;
        }
        throw new IllegalArgumentException("Unknown LogLevel value: " + value);
    }
}
