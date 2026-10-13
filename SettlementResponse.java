package com.example.expensesplitter.dto;

import java.math.BigDecimal;

/** One payment needed to settle up: "fromUser pays amount to toUser". */
public record SettlementResponse(
        Long fromUserId,
        String fromUserName,
        Long toUserId,
        String toUserName,
        BigDecimal amount) {
}
