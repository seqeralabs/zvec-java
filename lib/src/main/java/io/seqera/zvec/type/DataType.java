package io.seqera.zvec.type;

public enum DataType {
    UNDEFINED(0),
    BINARY(1),
    STRING(2),
    BOOL(3),
    INT32(4),
    INT64(5),
    UINT32(6),
    UINT64(7),
    FLOAT(8),
    DOUBLE(9),
    VECTOR_BINARY32(20),
    VECTOR_BINARY64(21),
    VECTOR_FP16(22),
    VECTOR_FP32(23),
    VECTOR_FP64(24),
    VECTOR_INT4(25),
    VECTOR_INT8(26),
    VECTOR_INT16(27),
    SPARSE_VECTOR_FP16(30),
    SPARSE_VECTOR_FP32(31),
    ARRAY_BINARY(40),
    ARRAY_STRING(41),
    ARRAY_BOOL(42),
    ARRAY_INT32(43),
    ARRAY_INT64(44),
    ARRAY_UINT32(45),
    ARRAY_UINT64(46),
    ARRAY_FLOAT(47),
    ARRAY_DOUBLE(48);

    private final int value;

    DataType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean isVectorType() {
        return isDenseVectorType() || isSparseVectorType();
    }

    public boolean isDenseVectorType() {
        return value >= 20 && value <= 27;
    }

    public boolean isSparseVectorType() {
        return value >= 30 && value <= 31;
    }

    public boolean isArrayType() {
        return value >= 40 && value <= 48;
    }

    public boolean isScalarType() {
        return value >= 1 && value <= 9;
    }

    public static DataType fromValue(int value) {
        for (DataType dt : values()) {
            if (dt.value == value) return dt;
        }
        throw new IllegalArgumentException("Unknown DataType value: " + value);
    }
}
