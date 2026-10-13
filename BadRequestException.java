package com.example.expensesplitter.exception;

/** Thrown when the request is well-formed but breaks a business rule. Becomes HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
