package com.example.expensesplitter.service;

import com.example.expensesplitter.dto.CreateExpenseRequest;
import com.example.expensesplitter.dto.ExpenseResponse;
import com.example.expensesplitter.entity.Expense;
import com.example.expensesplitter.entity.ExpenseGroup;
import com.example.expensesplitter.entity.User;
import com.example.expensesplitter.exception.BadRequestException;
import com.example.expensesplitter.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Business logic for adding and listing expenses. */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;
    private final UserService userService;

    public ExpenseService(ExpenseRepository expenseRepository,
                          GroupService groupService,
                          UserService userService) {
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
        this.userService = userService;
    }

    /**
     * Adds an expense to a group. The cost is split equally between the participants
     * when balances are calculated (see BalanceService), so we only store who paid,
     * how much, and who shares it.
     */
    @Transactional
    public ExpenseResponse addExpense(Long groupId, CreateExpenseRequest request) {
        ExpenseGroup group = groupService.getGroupEntity(groupId); // 404 if the group doesn't exist

        // The payer must exist (404 otherwise) and must belong to the group (400 otherwise).
        User payer = userService.getUserEntity(request.payerId());
        if (!group.hasMember(payer.getId())) {
            throw new BadRequestException("Payer " + payer.getId() + " is not a member of group " + groupId);
        }

        // Remove duplicate ids, then check every participant the same way.
        Set<User> participants = new HashSet<>();
        for (Long participantId : new LinkedHashSet<>(request.participantIds())) {
            User participant = userService.getUserEntity(participantId);
            if (!group.hasMember(participantId)) {
                throw new BadRequestException("Participant " + participantId + " is not a member of group " + groupId);
            }
            participants.add(participant);
        }

        // Always store exactly 2 decimals, e.g. 10 -> 10.00
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

        Expense expense = new Expense(group, payer, request.description().trim(), amount, participants);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(Long groupId) {
        groupService.getGroupEntity(groupId); // 404 if the group doesn't exist
        return expenseRepository.findByGroupIdOrderByIdAsc(groupId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }
}
