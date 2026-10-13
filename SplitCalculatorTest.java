package com.example.expensesplitter.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit tests for splitting an amount equally, to the cent. */
class SplitCalculatorTest {

    private static BigDecimal sum(Map<Long, BigDecimal> shares) {
        return shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("Even split: 90.00 between 3 people is 30.00 each")
    void evenSplit() {
        Map<Long, BigDecimal> shares = SplitCalculator.splitEqually(new BigDecimal("90.00"), List.of(1L, 2L, 3L));

        shares.values().forEach(share -> assertEquals(0, new BigDecimal("30.00").compareTo(share)));
    }

    @Test
    @DisplayName("Uneven split: 100.00 between 3 people -> 33.34, 33.33, 33.33 (extra cent to the first)")
    void unevenSplit() {
        Map<Long, BigDecimal> shares = SplitCalculator.splitEqually(new BigDecimal("100.00"), List.of(1L, 2L, 3L));

        assertEquals(new BigDecimal("33.34"), shares.get(1L));
        assertEquals(new BigDecimal("33.33"), shares.get(2L));
        assertEquals(new BigDecimal("33.33"), shares.get(3L));
        assertEquals(0, new BigDecimal("100.00").compareTo(sum(shares)));
    }

    @Test
    @DisplayName("Shares always add up to the exact amount")
    void sharesAlwaysAddUp() {
        String[] amounts = {"0.01", "0.02", "10.00", "99.99", "100.00", "1234.56", "0.10"};
        for (String amount : amounts) {
            for (int people = 1; people <= 9; people++) {
                List<Long> ids = java.util.stream.LongStream.rangeClosed(1, people).boxed().toList();
                Map<Long, BigDecimal> shares = SplitCalculator.splitEqually(new BigDecimal(amount), ids);
                assertEquals(0, new BigDecimal(amount).compareTo(sum(shares)),
                        amount + " split between " + people + " people does not add up");
            }
        }
    }

    @Test
    @DisplayName("One participant pays the whole amount")
    void singleParticipant() {
        Map<Long, BigDecimal> shares = SplitCalculator.splitEqually(new BigDecimal("42.50"), List.of(7L));

        assertEquals(new BigDecimal("42.50"), shares.get(7L));
    }

    @Test
    @DisplayName("No participants is an error")
    void noParticipants() {
        assertThrows(IllegalArgumentException.class,
                () -> SplitCalculator.splitEqually(new BigDecimal("10.00"), List.of()));
    }
}
