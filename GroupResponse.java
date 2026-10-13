package com.example.expensesplitter.dto;

import com.example.expensesplitter.entity.ExpenseGroup;
import com.example.expensesplitter.entity.User;

import java.util.Comparator;
import java.util.List;

/** A group with its members, as returned by the API. */
public record GroupResponse(Long id, String name, List<UserResponse> members) {

    public static GroupResponse from(ExpenseGroup group) {
        List<UserResponse> members = group.getMembers().stream()
                .sorted(Comparator.comparing(User::getId)) // stable order in the JSON
                .map(UserResponse::from)
                .toList();
        return new GroupResponse(group.getId(), group.getName(), members);
    }
}
