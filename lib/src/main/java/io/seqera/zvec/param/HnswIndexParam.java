package io.seqera.zvec.param;

import io.seqera.zvec.internal.MemoryUtils;
import io.seqera.zvec.internal.ZvecBindings;
import io.seqera.zvec.type.MetricType;
import io.seqera.zvec.type.QuantizeType;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class HnswIndexParam implements IndexParam {
    private final MetricType metricType;
    private final int m;
    private final int efConstruction;
    private final QuantizeType quantizeType;

    public HnswIndexParam(MetricType metricType) {
        this(metricType, 50, 500, QuantizeType.UNDEFINED);
    }

    public HnswIndexParam(MetricType metricType, int m, int efConstruction) {
        this(metricType, m, efConstruction, QuantizeType.UNDEFINED);
    }

    public HnswIndexParam(MetricType metricType, int m, int efConstruction, QuantizeType quantizeType) {
        this.metricType = metricType;
        this.m = m;
        this.efConstruction = efConstruction;
        this.quantizeType = quantizeType;
    }

    public MetricType metricType() { return metricType; }
    public int m() { return m; }
    public int efConstruction() { return efConstruction; }
    public QuantizeType quantizeType() { return quantizeType; }

    @Override
    public MemorySegment createNative(Arena arena) {
        try {
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_hnsw_index_params_create.invokeExact(
                    metricType.value(), m, efConstruction, quantizeType.value(), out);
            MemoryUtils.checkStatus(rc);
            return MemoryUtils.readOutPointer(out);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create HnswIndexParam", t);
        }
    }
}
