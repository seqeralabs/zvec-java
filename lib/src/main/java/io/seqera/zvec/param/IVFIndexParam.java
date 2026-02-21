package io.seqera.zvec.param;

import io.seqera.zvec.internal.MemoryUtils;
import io.seqera.zvec.internal.ZvecBindings;
import io.seqera.zvec.type.MetricType;
import io.seqera.zvec.type.QuantizeType;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class IVFIndexParam implements IndexParam {
    private final MetricType metricType;
    private final int nList;
    private final int nIters;
    private final boolean useSoar;
    private final QuantizeType quantizeType;

    public IVFIndexParam(MetricType metricType) {
        this(metricType, 1024, 10, false, QuantizeType.UNDEFINED);
    }

    public IVFIndexParam(MetricType metricType, int nList, int nIters, boolean useSoar) {
        this(metricType, nList, nIters, useSoar, QuantizeType.UNDEFINED);
    }

    public IVFIndexParam(MetricType metricType, int nList, int nIters, boolean useSoar, QuantizeType quantizeType) {
        this.metricType = metricType;
        this.nList = nList;
        this.nIters = nIters;
        this.useSoar = useSoar;
        this.quantizeType = quantizeType;
    }

    public MetricType metricType() { return metricType; }
    public int nList() { return nList; }
    public int nIters() { return nIters; }
    public boolean useSoar() { return useSoar; }
    public QuantizeType quantizeType() { return quantizeType; }

    @Override
    public MemorySegment createNative(Arena arena) {
        try {
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_ivf_index_params_create.invokeExact(
                    metricType.value(), nList, nIters, useSoar ? 1 : 0, quantizeType.value(), out);
            MemoryUtils.checkStatus(rc);
            return MemoryUtils.readOutPointer(out);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create IVFIndexParam", t);
        }
    }
}
