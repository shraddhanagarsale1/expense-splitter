package com.example.expensesplitter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * A group of people who share expenses (for example "Goa Trip").
 * Named ExpenseGroup (not Group) and mapped to "expense_groups" because GROUP is an SQL keyword.
 */
@Entity
@Table(name = "expense_groups")
public class ExpenseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * A user can be in many groups and a group has many users -> many-to-many.
     * JPA stores this in a join table "group_members" with two columns: group_id and user_id.
     */
    @ManyToMany
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members = new HashSet<>();

    protected ExpenseGroup() {
    }

    public ExpenseGroup(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void addMember(User user) {
        members.add(user);
    }

    public void removeMember(User user) {
        members.remove(user);
    }

    /** True if a user with this id belongs to the group. */
    public boolean hasMember(Long userId) {
        return members.stream().anyMatch(member -> member.getId().equals(userId));
    }
}
