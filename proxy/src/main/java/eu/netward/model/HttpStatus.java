package eu.netward.model;

public enum HttpStatus {
    NOT_FOUND(404, "Not Found"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    FORBIDDEN(403, "Forbidden");

    private final int code;
    private final String reasonPhrase;

    HttpStatus(int code, String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }

    public int getCode() {
        return code;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }
}
