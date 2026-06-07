package com.mathewshereni.open_aid_transparency.common.exception;

/**
 * Thrown when creating/updating would violate a uniqueness rule (e.g. two donors
 * with the same name). Our global handler turns this into an HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
