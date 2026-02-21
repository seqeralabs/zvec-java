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

package io.seqera.zvec.param;

import io.seqera.zvec.internal.MemoryUtils;
import io.seqera.zvec.internal.ZvecBindings;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class HnswQueryParam implements QueryParam {
    private final int ef;
    private final float radius;
    private final boolean isLinear;
    private final boolean isUsingRefiner;

    public HnswQueryParam() {
        this(300, 0.0f, false, false);
    }

    public HnswQueryParam(int ef) {
        this(ef, 0.0f, false, false);
    }

    public HnswQueryParam(int ef, float radius, boolean isLinear, boolean isUsingRefiner) {
        this.ef = ef;
        this.radius = radius;
        this.isLinear = isLinear;
        this.isUsingRefiner = isUsingRefiner;
    }

    public int ef() { return ef; }
    public float radius() { return radius; }
    public boolean isLinear() { return isLinear; }
    public boolean isUsingRefiner() { return isUsingRefiner; }

    @Override
    public MemorySegment createNative(Arena arena) {
        try {
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_hnsw_query_params_create.invokeExact(
                    ef, radius, isLinear ? 1 : 0, isUsingRefiner ? 1 : 0, out);
            MemoryUtils.checkStatus(rc);
            return MemoryUtils.readOutPointer(out);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create HnswQueryParam", t);
        }
    }
}
