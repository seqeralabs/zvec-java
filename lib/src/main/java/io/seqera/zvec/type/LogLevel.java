package io.seqera.zvec.type;

public enum LogLevel {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    FATAL(4);

    private final int value;

    LogLevel(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static LogLevel fromValue(int value) {
        for (LogLevel ll : values()) {
            if (ll.value == value) return ll;
        }
        throw new IllegalArgumentException("Unknown LogLevel value: " + value);
    }
}
