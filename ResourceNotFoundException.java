package com.example.expensesplitter.exception;

/** Thrown when something we are asked for does not exist. Becomes HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
