package com.example.expensesplitter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Body of POST /groups.
 * memberIds is optional: you can create an empty group and add members later.
 */
public record CreateGroupRequest(
        @NotBlank(message = "name is required") String name,
        List<@NotNull(message = "memberIds must not contain null") Long> memberIds) {
}
