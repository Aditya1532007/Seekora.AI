package com.seekora.service;

/** Thrown for business rule violations; servlets turn it into a 4xx response. */
public class ServiceException extends RuntimeException {

    private final int statusCode;

    public ServiceException(String message) {
        this(message, 400);
    }

    public ServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
