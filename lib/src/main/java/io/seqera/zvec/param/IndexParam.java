package io.seqera.zvec.param;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public interface IndexParam {
    MemorySegment createNative(Arena arena);
}
