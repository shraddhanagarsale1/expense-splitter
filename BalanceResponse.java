package com.example.expensesplitter.dto;

import java.math.BigDecimal;

/**
 * Net balance of one user inside a group.
 *  balance > 0 : the group owes this user money (they paid more than their share)
 *  balance < 0 : this user owes the group money (they paid less than their share)
 *  balance = 0 : all square
 */
public record BalanceResponse(Long userId, String userName, BigDecimal balance) {
}
