package com.example.expensesplitter.dto;

import com.example.expensesplitter.entity.Expense;
import com.example.expensesplitter.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** An expense as returned by the API, including who paid and who shares it. */
public record ExpenseResponse(
        Long id,
        Long groupId,
        String description,
        BigDecimal amount,
        UserResponse payer,
        List<UserResponse> participants,
        LocalDateTime createdAt) {

    public static ExpenseResponse from(Expense expense) {
        List<UserResponse> participants = expense.getParticipants().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(UserResponse::from)
                .toList();
        return new ExpenseResponse(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getDescription(),
                expense.getAmount(),
                UserResponse.from(expense.getPayer()),
                participants,
                expense.getCreatedAt());
    }
}
