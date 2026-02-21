package io.seqera.zvec;

import io.seqera.zvec.type.StatusCode;

public class ZvecException extends RuntimeException {
    private final StatusCode statusCode;

    public ZvecException(StatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public StatusCode statusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return "ZvecException[" + statusCode + "]: " + getMessage();
    }
}
