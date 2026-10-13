package com.example.expensesplitter.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The JSON body returned for every error, so clients always see the same shape.
 * A record is a short way to write an immutable class with a constructor and getters.
 * fieldErrors is only present for validation failures (null values are left out of the JSON).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}
