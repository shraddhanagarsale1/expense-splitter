package com.example.expensesplitter.controller;

import com.example.expensesplitter.dto.CreateExpenseRequest;
import com.example.expensesplitter.dto.ExpenseResponse;
import com.example.expensesplitter.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Expenses live inside a group, so the URLs start with /groups/{groupId}. */
@RestController
@RequestMapping("/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /** POST /groups/{groupId}/expenses -> 201 Created */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse addExpense(@PathVariable("groupId") Long groupId,
                                      @Valid @RequestBody CreateExpenseRequest request) {
        return expenseService.addExpense(groupId, request);
    }

    /** GET /groups/{groupId}/expenses -> 200 OK */
    @GetMapping
    public List<ExpenseResponse> getExpenses(@PathVariable("groupId") Long groupId) {
        return expenseService.getExpenses(groupId);
    }
}
