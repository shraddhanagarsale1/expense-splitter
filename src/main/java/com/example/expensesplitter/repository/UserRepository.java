package com.example.expensesplitter.repository;

import com.example.expensesplitter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA writes the implementation for us at startup.
 * JpaRepository already gives save(), findById(), findAll(), delete() ...
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Spring builds the query from the method name: SELECT COUNT(*) > 0 FROM users WHERE email = ? */
    boolean existsByEmail(String email);
}
