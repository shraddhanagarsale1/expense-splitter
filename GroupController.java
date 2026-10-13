package com.example.expensesplitter.controller;

import com.example.expensesplitter.dto.AddMemberRequest;
import com.example.expensesplitter.dto.CreateGroupRequest;
import com.example.expensesplitter.dto.GroupResponse;
import com.example.expensesplitter.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints for groups and their members. */
@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /** POST /groups -> 201 Created */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request);
    }

    /** GET /groups -> 200 OK */
    @GetMapping
    public List<GroupResponse> getAllGroups() {
        return groupService.getAllGroups();
    }

    /** GET /groups/{id} -> 200 OK, or 404 */
    @GetMapping("/{id}")
    public GroupResponse getGroup(@PathVariable("id") Long id) {
        return groupService.getGroup(id);
    }

    /** POST /groups/{id}/members -> 200 OK with the updated group */
    @PostMapping("/{id}/members")
    public GroupResponse addMember(@PathVariable("id") Long id,
                                   @Valid @RequestBody AddMemberRequest request) {
        return groupService.addMember(id, request.userId());
    }

    /** DELETE /groups/{id}/members/{userId} -> 200 OK with the updated group */
    @DeleteMapping("/{id}/members/{userId}")
    public GroupResponse removeMember(@PathVariable("id") Long id,
                                      @PathVariable("userId") Long userId) {
        return groupService.removeMember(id, userId);
    }
}
