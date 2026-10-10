package com.seekora.service;

/** Thrown for business rule violations; servlets turn it into a 4xx response. */
public class ServiceException extends RuntimeException {

    private final int statusCode;
    private final String diagnosticMessage;

    public ServiceException(String message) {
        this(message, 400);
    }

    public ServiceException(String message, int statusCode) {
        this(message, statusCode, null);
    }

    public ServiceException(String message, int statusCode, String diagnosticMessage) {
        super(message);
        this.statusCode = statusCode;
        this.diagnosticMessage = diagnosticMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getDiagnosticMessage() {
        return diagnosticMessage;
    }
}
