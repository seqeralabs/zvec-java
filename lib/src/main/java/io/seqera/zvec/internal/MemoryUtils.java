package io.seqera.zvec.internal;

import io.seqera.zvec.ZvecException;
import io.seqera.zvec.type.StatusCode;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public final class MemoryUtils {
    private MemoryUtils() {}

    public static MemorySegment toCString(Arena arena, String s) {
        return arena.allocateFrom(s);
    }

    public static String fromCString(MemorySegment segment) {
        if (segment.equals(MemorySegment.NULL) || segment.address() == 0) return null;
        return segment.reinterpret(Long.MAX_VALUE).getString(0);
    }

    public static MemorySegment toFloatArray(Arena arena, float[] data) {
        var segment = arena.allocate(ValueLayout.JAVA_FLOAT, data.length);
        MemorySegment.copy(data, 0, segment, ValueLayout.JAVA_FLOAT, 0, data.length);
        return segment;
    }

    public static float[] fromFloatArray(MemorySegment segment, int count) {
        return segment.reinterpret((long) count * ValueLayout.JAVA_FLOAT.byteSize())
                .toArray(ValueLayout.JAVA_FLOAT);
    }

    public static MemorySegment toDoubleArray(Arena arena, double[] data) {
        var segment = arena.allocate(ValueLayout.JAVA_DOUBLE, data.length);
        MemorySegment.copy(data, 0, segment, ValueLayout.JAVA_DOUBLE, 0, data.length);
        return segment;
    }

    public static double[] fromDoubleArray(MemorySegment segment, int count) {
        return segment.reinterpret((long) count * ValueLayout.JAVA_DOUBLE.byteSize())
                .toArray(ValueLayout.JAVA_DOUBLE);
    }

    public static MemorySegment toIntArray(Arena arena, int[] data) {
        var segment = arena.allocate(ValueLayout.JAVA_INT, data.length);
        MemorySegment.copy(data, 0, segment, ValueLayout.JAVA_INT, 0, data.length);
        return segment;
    }

    public static int[] fromIntArray(MemorySegment segment, int count) {
        return segment.reinterpret((long) count * ValueLayout.JAVA_INT.byteSize())
                .toArray(ValueLayout.JAVA_INT);
    }

    public static MemorySegment toLongArray(Arena arena, long[] data) {
        var segment = arena.allocate(ValueLayout.JAVA_LONG, data.length);
        MemorySegment.copy(data, 0, segment, ValueLayout.JAVA_LONG, 0, data.length);
        return segment;
    }

    public static long[] fromLongArray(MemorySegment segment, int count) {
        return segment.reinterpret((long) count * ValueLayout.JAVA_LONG.byteSize())
                .toArray(ValueLayout.JAVA_LONG);
    }

    public static MemorySegment toPointerArray(Arena arena, MemorySegment[] segments) {
        var arr = arena.allocate(ValueLayout.ADDRESS, segments.length);
        for (int i = 0; i < segments.length; i++) {
            arr.setAtIndex(ValueLayout.ADDRESS, i, segments[i]);
        }
        return arr;
    }

    public static MemorySegment toStringArray(Arena arena, String[] strings) {
        var arr = arena.allocate(ValueLayout.ADDRESS, strings.length);
        for (int i = 0; i < strings.length; i++) {
            arr.setAtIndex(ValueLayout.ADDRESS, i, arena.allocateFrom(strings[i]));
        }
        return arr;
    }

    public static MemorySegment allocateOutPointer(Arena arena) {
        return arena.allocate(ValueLayout.ADDRESS);
    }

    public static MemorySegment readOutPointer(MemorySegment out) {
        return out.get(ValueLayout.ADDRESS, 0);
    }

    public static MemorySegment allocateOutInt(Arena arena) {
        return arena.allocate(ValueLayout.JAVA_INT);
    }

    public static int readOutInt(MemorySegment out) {
        return out.get(ValueLayout.JAVA_INT, 0);
    }

    public static MemorySegment allocateOutLong(Arena arena) {
        return arena.allocate(ValueLayout.JAVA_LONG);
    }

    public static long readOutLong(MemorySegment out) {
        return out.get(ValueLayout.JAVA_LONG, 0);
    }

    public static MemorySegment allocateOutFloat(Arena arena) {
        return arena.allocate(ValueLayout.JAVA_FLOAT);
    }

    public static float readOutFloat(MemorySegment out) {
        return out.get(ValueLayout.JAVA_FLOAT, 0);
    }

    public static void checkStatus(int code) {
        if (code == 0) return;
        String msg;
        try {
            var msgSeg = (MemorySegment) ZvecBindings.zvec_last_error_message.invokeExact();
            msg = fromCString(msgSeg);
        } catch (Throwable t) {
            msg = "Failed to get error message";
        }
        throw new ZvecException(StatusCode.fromValue(code), msg != null ? msg : "Unknown error (code " + code + ")");
    }

    public static String readAndFreeString(MemorySegment ptrOut) {
        var ptr = readOutPointer(ptrOut);
        if (ptr.address() == 0) return null;
        String result = fromCString(ptr);
        try {
            ZvecBindings.zvec_free_string.invokeExact(ptr);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to free native string", t);
        }
        return result;
    }
}
