package com.seekora.dao;

/** Unchecked wrapper around {@link java.sql.SQLException}. */
public class DaoException extends RuntimeException {

    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }

    public DaoException(String message) {
        super(message);
    }
}
