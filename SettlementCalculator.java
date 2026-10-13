package com.example.expensesplitter.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Works out WHO should pay WHOM so that everybody ends up with a zero balance,
 * using as few payments as the greedy method can manage.
 *
 * Input : net balance per user. Positive = is owed money (creditor), negative = owes money (debtor).
 *         All balances together must add up to exactly zero.
 * Output: a list of payments (debtor -> creditor, amount).
 *
 * Algorithm (two heaps, see the README for a worked example):
 *   1. Put every creditor in a max-heap and every debtor in a max-heap (largest amount on top).
 *   2. Repeat: take the biggest creditor and the biggest debtor.
 *      The debtor pays the smaller of the two amounts to the creditor.
 *      That settles at least one of them completely.
 *      Whoever still has a remainder goes back on their heap.
 *   3. Stop when the heaps are empty.
 *
 * Every payment settles at least one person, so for n people with a non-zero balance
 * we need at most n - 1 payments. Time complexity: O(n log n).
 *
 * This class has no database access, so it is simple to unit test.
 */
@Component
public class SettlementCalculator {

    /** One payment: "fromUserId pays amount to toUserId". */
    public record Transfer(Long fromUserId, Long toUserId, BigDecimal amount) {
    }

    /** A user with a positive amount (credit or debt, depending on which heap holds it). */
    private record Entry(Long userId, BigDecimal amount) {
    }

    public List<Transfer> calculate(Map<Long, BigDecimal> balances) {
        // Sanity check: in a closed group, what is owed always equals what is due.
        BigDecimal total = balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() != 0) {
            throw new IllegalArgumentException("Balances must add up to zero but add up to " + total);
        }

        // Largest amount first. If two amounts are equal, the lower userId goes first,
        // so the result is the same every time (predictable output, easy to test).
        Comparator<Entry> largestFirst =
                Comparator.comparing(Entry::amount, Comparator.<BigDecimal>reverseOrder())
                        .thenComparing(Entry::userId);

        PriorityQueue<Entry> creditors = new PriorityQueue<>(largestFirst); // people who are owed
        PriorityQueue<Entry> debtors = new PriorityQueue<>(largestFirst);   // people who owe

        for (Map.Entry<Long, BigDecimal> balance : balances.entrySet()) {
            if (balance.getValue().signum() > 0) {
                creditors.add(new Entry(balance.getKey(), balance.getValue()));
            } else if (balance.getValue().signum() < 0) {
                // store the debt as a positive number so both heaps work the same way
                debtors.add(new Entry(balance.getKey(), balance.getValue().negate()));
            }
            // balance == 0 -> nothing to do for this person
        }

        List<Transfer> transfers = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Entry creditor = creditors.poll(); // owed the most
            Entry debtor = debtors.poll();     // owes the most

            // The debtor pays as much as possible without overpaying the creditor.
            BigDecimal payment = creditor.amount().min(debtor.amount());
            transfers.add(new Transfer(debtor.userId(), creditor.userId(), payment));

            BigDecimal creditorLeft = creditor.amount().subtract(payment);
            BigDecimal debtorLeft = debtor.amount().subtract(payment);

            // At least one of them is now at zero. Anyone with money left goes back in the heap.
            if (creditorLeft.signum() > 0) {
                creditors.add(new Entry(creditor.userId(), creditorLeft));
            }
            if (debtorLeft.signum() > 0) {
                debtors.add(new Entry(debtor.userId(), debtorLeft));
            }
        }

        return transfers;
    }
}
