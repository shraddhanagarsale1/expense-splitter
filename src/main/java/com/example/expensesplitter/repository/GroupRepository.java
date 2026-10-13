package com.example.expensesplitter.repository;

import com.example.expensesplitter.entity.ExpenseGroup;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for groups. The built-in methods are all we need. */
public interface GroupRepository extends JpaRepository<ExpenseGroup, Long> {
}
