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

public class IVFQueryParam implements QueryParam {
    private final int nprobe;
    private final boolean isUsingRefiner;
    private final float scaleFactor;

    public IVFQueryParam() {
        this(10, false, 10.0f);
    }

    public IVFQueryParam(int nprobe, boolean isUsingRefiner, float scaleFactor) {
        this.nprobe = nprobe;
        this.isUsingRefiner = isUsingRefiner;
        this.scaleFactor = scaleFactor;
    }

    public int nprobe() { return nprobe; }
    public boolean isUsingRefiner() { return isUsingRefiner; }
    public float scaleFactor() { return scaleFactor; }

    @Override
    public MemorySegment createNative(Arena arena) {
        try {
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_ivf_query_params_create.invokeExact(
                    nprobe, isUsingRefiner ? 1 : 0, scaleFactor, out);
            MemoryUtils.checkStatus(rc);
            return MemoryUtils.readOutPointer(out);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create IVFQueryParam", t);
        }
    }
}
