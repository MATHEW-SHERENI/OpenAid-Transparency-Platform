package com.mathewshereni.open_aid_transparency.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * The single, consistent JSON shape we return for ALL errors.
 *
 * @JsonInclude(NON_NULL) means null fields are omitted from the JSON - so a
 * simple 404 won't carry an empty "fieldErrors" key, but a validation 400 will.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> fieldErrors
) {
    /** One per invalid field, e.g. {"field":"name","message":"must not be blank"}. */
    public record FieldValidationError(String field, String message) {
    }
}
