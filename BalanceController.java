package com.example.expensesplitter.controller;

import com.example.expensesplitter.dto.BalanceResponse;
import com.example.expensesplitter.dto.SettlementResponse;
import com.example.expensesplitter.service.BalanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only endpoints: who owes what, and how to settle up. */
@RestController
@RequestMapping("/groups/{groupId}")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    /** GET /groups/{groupId}/balances */
    @GetMapping("/balances")
    public List<BalanceResponse> getBalances(@PathVariable("groupId") Long groupId) {
        return balanceService.getBalances(groupId);
    }

    /** GET /groups/{groupId}/settlements */
    @GetMapping("/settlements")
    public List<SettlementResponse> getSettlements(@PathVariable("groupId") Long groupId) {
        return balanceService.getSettlements(groupId);
    }
}
