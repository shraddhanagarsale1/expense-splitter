package com.example.expensesplitter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * One expense inside a group: "Alice paid 3000 for dinner, shared by Alice, Bob and Charlie".
 */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    /**
     * Money is stored as BigDecimal, never double: doubles cannot represent 0.1 exactly,
     * which leads to rounding bugs. precision=12, scale=2 -> up to 10 digits before the point, 2 after.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Many expenses belong to one group. LAZY = don't load the group until it is needed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    /** The person who actually paid the bill. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id")
    private User payer;

    /** The people who share the cost (the payer may or may not be one of them). */
    @ManyToMany
    @JoinTable(
            name = "expense_participants",
            joinColumns = @JoinColumn(name = "expense_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> participants = new HashSet<>();

    protected Expense() {
    }

    public Expense(ExpenseGroup group, User payer, String description, BigDecimal amount, Set<User> participants) {
        this.group = group;
        this.payer = payer;
        this.description = description;
        this.amount = amount;
        this.participants = participants;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public User getPayer() {
        return payer;
    }

    public Set<User> getParticipants() {
        return participants;
    }
}
