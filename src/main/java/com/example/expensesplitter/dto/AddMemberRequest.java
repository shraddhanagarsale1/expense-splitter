package com.example.expensesplitter.dto;

import jakarta.validation.constraints.NotNull;

/** Body of POST /groups/{id}/members. */
public record AddMemberRequest(
        @NotNull(message = "userId is required") Long userId) {
}
