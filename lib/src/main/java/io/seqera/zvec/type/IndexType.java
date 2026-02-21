package io.seqera.zvec.type;

public enum IndexType {
    UNDEFINED(0),
    HNSW(1),
    IVF(3),
    FLAT(4),
    INVERT(10);

    private final int value;

    IndexType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static IndexType fromValue(int value) {
        for (IndexType t : values()) {
            if (t.value == value) return t;
        }
        throw new IllegalArgumentException("Unknown IndexType value: " + value);
    }
}
