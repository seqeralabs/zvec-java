package io.seqera.zvec.param;

import io.seqera.zvec.internal.MemoryUtils;
import io.seqera.zvec.internal.ZvecBindings;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class InvertIndexParam implements IndexParam {
    private final boolean enableRangeOptimization;
    private final boolean enableExtendedWildcard;

    public InvertIndexParam() {
        this(true, false);
    }

    public InvertIndexParam(boolean enableRangeOptimization, boolean enableExtendedWildcard) {
        this.enableRangeOptimization = enableRangeOptimization;
        this.enableExtendedWildcard = enableExtendedWildcard;
    }

    public boolean enableRangeOptimization() { return enableRangeOptimization; }
    public boolean enableExtendedWildcard() { return enableExtendedWildcard; }

    @Override
    public MemorySegment createNative(Arena arena) {
        try {
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_invert_index_params_create.invokeExact(
                    enableRangeOptimization ? 1 : 0, enableExtendedWildcard ? 1 : 0, out);
            MemoryUtils.checkStatus(rc);
            return MemoryUtils.readOutPointer(out);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create InvertIndexParam", t);
        }
    }
}
