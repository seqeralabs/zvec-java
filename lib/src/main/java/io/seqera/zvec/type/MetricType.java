package io.seqera.zvec.type;

public enum MetricType {
    UNDEFINED(0),
    L2(1),
    IP(2),
    COSINE(3),
    MIPSL2(4);

    private final int value;

    MetricType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static MetricType fromValue(int value) {
        for (MetricType t : values()) {
            if (t.value == value) return t;
        }
        throw new IllegalArgumentException("Unknown MetricType value: " + value);
    }
}
