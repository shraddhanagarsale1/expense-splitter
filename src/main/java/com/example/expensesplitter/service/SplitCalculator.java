package com.example.expensesplitter.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits an amount equally between people, to the cent.
 *
 * Problem: 100.00 split 3 ways is 33.333... which cannot be paid exactly.
 * Solution: give everyone 33.33 (rounded DOWN), then hand out the leftover cents
 * (100.00 - 3 x 33.33 = 0.01) one by one to the first people in the list.
 * Result: 33.34 + 33.33 + 33.33 = 100.00, so the shares ALWAYS add up to the exact amount.
 *
 * It is a plain utility class with no Spring annotations, which makes it easy to unit test.
 */
public final class SplitCalculator {

    private SplitCalculator() {
        // utility class: nobody should create an instance
    }

    /**
     * @param amount  total amount (at most 2 decimal places)
     * @param userIds the people who share the cost; must not be empty. The order decides who
     *                gets the extra cent(s), so callers pass them in a fixed (sorted) order.
     * @return userId -> share, in the same order as userIds
     */
    public static Map<Long, BigDecimal> splitEqually(BigDecimal amount, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required");
        }

        BigDecimal people = BigDecimal.valueOf(userIds.size());

        // 1) Base share, rounded down to cents. Example: 100.00 / 3 = 33.33
        BigDecimal baseShare = amount.divide(people, 2, RoundingMode.DOWN);

        // 2) Cents left over after everyone got the base share. Example: 100.00 - 99.99 = 0.01
        BigDecimal leftover = amount.subtract(baseShare.multiply(people));
        int extraCents = leftover.movePointRight(2).intValueExact(); // 0.01 -> 1 cent

        // 3) Hand out one extra cent to each of the first "extraCents" people.
        BigDecimal oneCent = new BigDecimal("0.01");
        Map<Long, BigDecimal> shares = new LinkedHashMap<>();
        for (int i = 0; i < userIds.size(); i++) {
            BigDecimal share = (i < extraCents) ? baseShare.add(oneCent) : baseShare;
            shares.put(userIds.get(i), share);
        }
        return shares;
    }
}
