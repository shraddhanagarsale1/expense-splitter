package com.example.expensesplitter.dto;

import com.example.expensesplitter.entity.User;

/** A user as returned by the API. */
public record UserResponse(Long id, String name, String email) {

    /** Converts an entity into its response DTO. */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
