package com.mathewshereni.open_aid_transparency.common.exception;

/**
 * Thrown when a requested record does not exist. Our global handler turns this
 * into an HTTP 404 response. Extending RuntimeException means callers are not
 * forced to wrap every call in try/catch.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
