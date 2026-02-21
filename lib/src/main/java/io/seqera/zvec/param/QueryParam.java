package io.seqera.zvec.param;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public interface QueryParam {
    MemorySegment createNative(Arena arena);
}
