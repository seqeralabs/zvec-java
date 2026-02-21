package io.seqera.zvec.type;

public enum QuantizeType {
    UNDEFINED(0),
    FP16(1),
    INT8(2),
    INT4(3);

    private final int value;

    QuantizeType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static QuantizeType fromValue(int value) {
        for (QuantizeType t : values()) {
            if (t.value == value) return t;
        }
        throw new IllegalArgumentException("Unknown QuantizeType value: " + value);
    }
}
