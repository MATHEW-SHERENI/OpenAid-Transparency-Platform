package com.mathewshereni.open_aid_transparency.donor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What clients SEND to create or update a donor. (Input shape.)
 *
 * The validation annotations are checked automatically when the controller marks
 * this parameter @Valid. If any fail, Spring throws before our code runs and the
 * GlobalExceptionHandler returns a 400 listing each bad field. Note there is no
 * 'id' here - the server assigns ids, clients never send them.
 */
public record DonorRequest(

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @NotNull(message = "type is required")
        DonorType type,

        @Size(max = 100, message = "country must be at most 100 characters")
        String country
) {
}
