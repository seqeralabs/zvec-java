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
import io.seqera.zvec.type.MetricType;
import io.seqera.zvec.type.QuantizeType;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class FlatIndexParam implements IndexParam {
    private final MetricType metricType;
    private final QuantizeType quantizeType;

    public FlatIndexParam(MetricType metricType) {
        this(metricType, QuantizeType.UNDEFINED);
    }

    public FlatIndexParam(MetricType metricType, QuantizeType quantizeType) {
        this.metricType = metricType;
        this.quantizeType = quantizeType;
    }

    public MetricType metricType() { return metricType; }
    public QuantizeType quantizeType() { return quantizeType; }

    @Override
    public MemorySegment createNative(Arena arena) {
        try {
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_flat_index_params_create.invokeExact(
                    metricType.value(), quantizeType.value(), out);
            MemoryUtils.checkStatus(rc);
            return MemoryUtils.readOutPointer(out);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create FlatIndexParam", t);
        }
    }
}
