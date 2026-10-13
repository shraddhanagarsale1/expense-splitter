package com.example.expensesplitter.service;

import com.example.expensesplitter.service.SettlementCalculator.Transfer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the settle-up algorithm.
 * No Spring and no database here: we create the calculator with "new" and call it directly,
 * so the tests run in milliseconds.
 */
class SettlementCalculatorTest {

    private final SettlementCalculator calculator = new SettlementCalculator();

    // ---------- helpers ----------

    /** Builds a balance map from alternating (userId, amount) pairs: balances(1, "50", 2, "-50"). */
    private static Map<Long, BigDecimal> balances(Object... idAndAmountPairs) {
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        for (int i = 0; i < idAndAmountPairs.length; i += 2) {
            map.put(((Number) idAndAmountPairs[i]).longValue(), new BigDecimal((String) idAndAmountPairs[i + 1]));
        }
        return map;
    }

    /** Compares money by value (so 50 equals 50.00). */
    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    /**
     * The most important check of all: if everybody makes the payments we suggest,
     * does every balance end up at exactly zero?
     */
    private static void assertEveryoneSettled(Map<Long, BigDecimal> startBalances, List<Transfer> transfers) {
        Map<Long, BigDecimal> after = new HashMap<>(startBalances);
        for (Transfer t : transfers) {
            assertTrue(t.amount().signum() > 0, "payments must be positive");
            after.merge(t.fromUserId(), t.amount(), BigDecimal::add);          // payer owes less
            after.merge(t.toUserId(), t.amount().negate(), BigDecimal::add);   // receiver is owed less
        }
        after.forEach((userId, balance) ->
                assertEquals(0, balance.signum(), "user " + userId + " is not settled: " + balance));
    }

    // ---------- tests ----------

    @Test
    @DisplayName("No balances -> no payments")
    void emptyBalances() {
        assertTrue(calculator.calculate(new HashMap<>()).isEmpty());
    }

    @Test
    @DisplayName("Everyone already at zero -> no payments")
    void allZero() {
        assertTrue(calculator.calculate(balances(1, "0.00", 2, "0.00", 3, "0.00")).isEmpty());
    }

    @Test
    @DisplayName("Two people: one payment")
    void twoPeople() {
        Map<Long, BigDecimal> input = balances(1, "50.00", 2, "-50.00");

        List<Transfer> result = calculator.calculate(input);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).fromUserId());
        assertEquals(1L, result.get(0).toUserId());
        assertMoney("50", result.get(0).amount());
    }

    @Test
    @DisplayName("One creditor, two debtors: debtors pay the creditor, largest debt first")
    void oneCreditorTwoDebtors() {
        Map<Long, BigDecimal> input = balances(1, "100.00", 2, "-70.00", 3, "-30.00");

        List<Transfer> result = calculator.calculate(input);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).fromUserId()); // biggest debtor goes first
        assertEquals(1L, result.get(0).toUserId());
        assertMoney("70", result.get(0).amount());
        assertEquals(3L, result.get(1).fromUserId());
        assertEquals(1L, result.get(1).toUserId());
        assertMoney("30", result.get(1).amount());
        assertEveryoneSettled(input, result);
    }

    @Test
    @DisplayName("Two creditors, two debtors: at most n-1 = 3 payments")
    void twoCreditorsTwoDebtors() {
        // A +50, B +30, C -45, D -35  (traced by hand in the README)
        Map<Long, BigDecimal> input = balances(1, "50.00", 2, "30.00", 3, "-45.00", 4, "-35.00");

        List<Transfer> result = calculator.calculate(input);

        assertEquals(3, result.size());
        assertEveryoneSettled(input, result);
    }

    @Test
    @DisplayName("Exact matches settle in a single payment each (no needless splitting)")
    void exactMatches() {
        Map<Long, BigDecimal> input = balances(1, "40.00", 2, "25.00", 3, "-25.00", 4, "-40.00");

        List<Transfer> result = calculator.calculate(input);

        assertEquals(2, result.size());
        assertEveryoneSettled(input, result);
    }

    @Test
    @DisplayName("People with a zero balance are left out of the payments")
    void zeroBalanceIsIgnored() {
        Map<Long, BigDecimal> input = balances(1, "20.00", 2, "0.00", 3, "-20.00");

        List<Transfer> result = calculator.calculate(input);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).fromUserId());
        assertEquals(1L, result.get(0).toUserId());
    }

    @Test
    @DisplayName("Cents are handled exactly (no floating point errors)")
    void decimalAmounts() {
        Map<Long, BigDecimal> input = balances(1, "66.67", 2, "-33.34", 3, "-33.33");

        List<Transfer> result = calculator.calculate(input);

        assertEquals(2, result.size());
        assertMoney("33.34", result.get(0).amount());
        assertMoney("33.33", result.get(1).amount());
        assertEveryoneSettled(input, result);
    }

    @Test
    @DisplayName("Big group: never more than n-1 payments, and everyone ends at zero")
    void biggerGroup() {
        Map<Long, BigDecimal> input = balances(
                1, "120.50", 2, "-40.25", 3, "75.00", 4, "-10.00", 5, "-60.75",
                6, "-30.00", 7, "15.00", 8, "-69.50");
        // sanity: the test data itself must add up to zero
        assertEquals(0, input.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).signum());

        List<Transfer> result = calculator.calculate(input);

        assertTrue(result.size() <= input.size() - 1, "got " + result.size() + " payments");
        assertEveryoneSettled(input, result);
    }

    @Test
    @DisplayName("Balances that do not add up to zero are rejected")
    void balancesNotZeroSum() {
        Map<Long, BigDecimal> input = balances(1, "50.00", 2, "-40.00");

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(input));
    }
}
