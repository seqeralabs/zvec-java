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
