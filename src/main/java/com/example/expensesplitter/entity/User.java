package com.example.expensesplitter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * A person who can join groups and pay for / share expenses.
 * Maps to the "users" table. (We avoid the table name "user" because it is
 * a reserved word in several databases.)
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // the database auto-increments the id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true) // two users cannot share an email
    private String email;

    /** JPA needs a no-argument constructor. "protected" stops other code from using it by mistake. */
    protected User() {
    }

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Two users are "the same" if they have the same id.
     * This matters because we store users in a Set (members of a group, participants of an expense).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
