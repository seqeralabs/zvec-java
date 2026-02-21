package io.seqera.zvec.param;

import io.seqera.zvec.internal.MemoryUtils;
import io.seqera.zvec.internal.ZvecBindings;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class FlatQueryParam implements QueryParam {
    private final boolean isUsingRefiner;
    private final float scaleFactor;

    public FlatQueryParam() {
        this(false, 10.0f);
    }

    public FlatQueryParam(boolean isUsingRefiner, float scaleFactor) {
        this.isUsingRefiner = isUsingRefiner;
        this.scaleFactor = scaleFactor;
    }

    public boolean isUsingRefiner() { return isUsingRefiner; }
    public float scaleFactor() { return scaleFactor; }

    @Override
    public MemorySegment createNative(Arena arena) {
        try {
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_flat_query_params_create.invokeExact(
                    isUsingRefiner ? 1 : 0, scaleFactor, out);
            MemoryUtils.checkStatus(rc);
            return MemoryUtils.readOutPointer(out);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create FlatQueryParam", t);
        }
    }
}
