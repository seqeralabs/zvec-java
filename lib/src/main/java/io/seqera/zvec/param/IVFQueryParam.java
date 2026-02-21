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
