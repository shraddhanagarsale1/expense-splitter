package com.example.expensesplitter.repository;

import com.example.expensesplitter.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * "GroupId" is read as the "id" property of the "group" field in Expense,
     * so this runs: SELECT * FROM expenses WHERE group_id = ? ORDER BY id ASC
     */
    List<Expense> findByGroupIdOrderByIdAsc(Long groupId);
}
