package io.seqera.zvec.type;

public enum StatusCode {
    OK(0),
    NOT_FOUND(1),
    ALREADY_EXISTS(2),
    INVALID_ARGUMENT(3),
    PERMISSION_DENIED(4),
    FAILED_PRECONDITION(5),
    RESOURCE_EXHAUSTED(6),
    UNAVAILABLE(7),
    INTERNAL_ERROR(8),
    NOT_SUPPORTED(9),
    UNKNOWN(10);

    private final int value;

    StatusCode(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static StatusCode fromValue(int value) {
        for (StatusCode sc : values()) {
            if (sc.value == value) return sc;
        }
        throw new IllegalArgumentException("Unknown StatusCode value: " + value);
    }
}
