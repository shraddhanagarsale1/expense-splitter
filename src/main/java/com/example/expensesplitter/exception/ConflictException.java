package com.example.expensesplitter.exception;

/** Thrown when the request clashes with the current state of the data. Becomes HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
