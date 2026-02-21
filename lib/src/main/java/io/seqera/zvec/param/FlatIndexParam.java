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
