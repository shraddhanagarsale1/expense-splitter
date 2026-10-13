package com.example.expensesplitter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /users.
 * DTOs (Data Transfer Objects) are the shapes of the JSON we accept and return.
 * We use them instead of entities so the API doesn't expose the database model directly.
 * The annotations are checked automatically because the controller uses @Valid.
 */
public record CreateUserRequest(
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "email is required") @Email(message = "email must be a valid email address") String email) {
}
