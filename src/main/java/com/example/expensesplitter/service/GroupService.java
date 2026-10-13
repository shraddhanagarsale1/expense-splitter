package com.example.expensesplitter.service;

import com.example.expensesplitter.dto.CreateGroupRequest;
import com.example.expensesplitter.dto.GroupResponse;
import com.example.expensesplitter.entity.Expense;
import com.example.expensesplitter.entity.ExpenseGroup;
import com.example.expensesplitter.entity.User;
import com.example.expensesplitter.exception.ConflictException;
import com.example.expensesplitter.exception.ResourceNotFoundException;
import com.example.expensesplitter.repository.ExpenseRepository;
import com.example.expensesplitter.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

/** Business logic for groups and their members. */
@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final UserService userService;

    public GroupService(GroupRepository groupRepository,
                        ExpenseRepository expenseRepository,
                        UserService userService) {
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.userService = userService;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        ExpenseGroup group = new ExpenseGroup(request.name().trim());

        if (request.memberIds() != null) {
            // LinkedHashSet removes duplicate ids but keeps the order.
            for (Long userId : new LinkedHashSet<>(request.memberIds())) {
                group.addMember(userService.getUserEntity(userId)); // 404 if a user doesn't exist
            }
        }
        return GroupResponse.from(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroups() {
        return groupRepository.findAll().stream().map(GroupResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long groupId) {
        return GroupResponse.from(getGroupEntity(groupId));
    }

    @Transactional
    public GroupResponse addMember(Long groupId, Long userId) {
        ExpenseGroup group = getGroupEntity(groupId);
        User user = userService.getUserEntity(userId);

        if (group.hasMember(userId)) {
            throw new ConflictException("User " + userId + " is already a member of group " + groupId);
        }
        group.addMember(user);
        return GroupResponse.from(groupRepository.save(group));
    }

    @Transactional
    public GroupResponse removeMember(Long groupId, Long userId) {
        ExpenseGroup group = getGroupEntity(groupId);

        if (!group.hasMember(userId)) {
            throw new ResourceNotFoundException("User " + userId + " is not a member of group " + groupId);
        }

        // Business rule: a member who is part of any expense cannot leave, because deleting
        // them would silently change everybody else's balances. Settle up first.
        if (isInvolvedInAnyExpense(groupId, userId)) {
            throw new ConflictException("User " + userId + " is part of existing expenses in group "
                    + groupId + " and cannot be removed");
        }

        group.removeMember(userService.getUserEntity(userId));
        return GroupResponse.from(groupRepository.save(group));
    }

    /** Used by other services. Throws 404 if the group doesn't exist. */
    @Transactional(readOnly = true)
    public ExpenseGroup getGroupEntity(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + groupId + " not found"));
    }

    /** True if the user paid for, or shares the cost of, at least one expense in the group. */
    private boolean isInvolvedInAnyExpense(Long groupId, Long userId) {
        for (Expense expense : expenseRepository.findByGroupIdOrderByIdAsc(groupId)) {
            boolean isPayer = expense.getPayer().getId().equals(userId);
            boolean isParticipant = expense.getParticipants().stream()
                    .anyMatch(participant -> participant.getId().equals(userId));
            if (isPayer || isParticipant) {
                return true;
            }
        }
        return false;
    }
}
