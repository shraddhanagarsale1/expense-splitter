package com.example.expensesplitter.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body of POST /groups/{id}/expenses.
 * The amount is split EQUALLY between the participants.
 */
public record CreateExpenseRequest(
        @NotBlank(message = "description is required")
        String description,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "amount can have at most 10 digits and 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "payerId is required")
        Long payerId,

        @NotEmpty(message = "participantIds must contain at least one user")
        List<@NotNull(message = "participantIds must not contain null") Long> participantIds) {
}
