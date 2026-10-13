package com.example.expensesplitter.service;

import com.example.expensesplitter.dto.BalanceResponse;
import com.example.expensesplitter.dto.SettlementResponse;
import com.example.expensesplitter.entity.Expense;
import com.example.expensesplitter.entity.ExpenseGroup;
import com.example.expensesplitter.entity.User;
import com.example.expensesplitter.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calculates net balances and the settle-up plan for a group.
 *
 * Balances are NOT stored in the database. We recompute them from the expenses each time,
 * so they can never get out of sync with the real data.
 */
@Service
public class BalanceService {

    private final GroupService groupService;
    private final ExpenseRepository expenseRepository;
    private final SettlementCalculator settlementCalculator;

    public BalanceService(GroupService groupService,
                          ExpenseRepository expenseRepository,
                          SettlementCalculator settlementCalculator) {
        this.groupService = groupService;
        this.expenseRepository = expenseRepository;
        this.settlementCalculator = settlementCalculator;
    }

    /** Net balance of every member of the group. Positive = is owed money, negative = owes money. */
    @Transactional(readOnly = true)
    public List<BalanceResponse> getBalances(Long groupId) {
        ExpenseGroup group = groupService.getGroupEntity(groupId);
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByIdAsc(groupId);

        Map<Long, BigDecimal> balances = computeBalances(group, expenses);
        Map<Long, String> names = memberNames(group);

        return balances.entrySet().stream()
                .map(entry -> new BalanceResponse(
                        entry.getKey(),
                        names.getOrDefault(entry.getKey(), "User " + entry.getKey()),
                        entry.getValue()))
                .toList();
    }

    /** The payments that settle every debt in the group (see SettlementCalculator). */
    @Transactional(readOnly = true)
    public List<SettlementResponse> getSettlements(Long groupId) {
        ExpenseGroup group = groupService.getGroupEntity(groupId);
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByIdAsc(groupId);

        Map<Long, BigDecimal> balances = computeBalances(group, expenses);
        Map<Long, String> names = memberNames(group);

        return settlementCalculator.calculate(balances).stream()
                .map(transfer -> new SettlementResponse(
                        transfer.fromUserId(),
                        names.getOrDefault(transfer.fromUserId(), "User " + transfer.fromUserId()),
                        transfer.toUserId(),
                        names.getOrDefault(transfer.toUserId(), "User " + transfer.toUserId()),
                        transfer.amount()))
                .toList();
    }

    /**
     * Core idea of the balance calculation, for every expense:
     *   - the payer gets  + amount        (they put money in)
     *   - each participant gets - share   (they consumed their part)
     * Adding this up over all expenses gives each person's net balance.
     * Because every expense adds +amount and subtracts shares that sum to exactly amount,
     * all balances together always add up to zero.
     */
    private Map<Long, BigDecimal> computeBalances(ExpenseGroup group, List<Expense> expenses) {
        // TreeMap keeps users sorted by id, so the output order is stable.
        Map<Long, BigDecimal> balances = new TreeMap<>();

        // Everyone in the group starts at 0.00, so members without expenses still show up.
        for (User member : group.getMembers()) {
            balances.put(member.getId(), new BigDecimal("0.00"));
        }

        for (Expense expense : expenses) {
            // payer: + full amount
            balances.merge(expense.getPayer().getId(), expense.getAmount(), BigDecimal::add);

            // participants: - their share (sorted ids so the extra cent always goes to the same people)
            List<Long> participantIds = expense.getParticipants().stream()
                    .map(User::getId)
                    .sorted()
                    .toList();
            Map<Long, BigDecimal> shares = SplitCalculator.splitEqually(expense.getAmount(), participantIds);
            shares.forEach((userId, share) -> balances.merge(userId, share.negate(), BigDecimal::add));
        }
        return balances;
    }

    /** userId -> name for all members. (Members involved in expenses can't be removed, so this covers everyone.) */
    private Map<Long, String> memberNames(ExpenseGroup group) {
        Map<Long, String> names = new HashMap<>();
        for (User member : group.getMembers()) {
            names.put(member.getId(), member.getName());
        }
        return names;
    }
}
