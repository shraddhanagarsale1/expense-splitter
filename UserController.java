package com.example.expensesplitter.controller;

import com.example.expensesplitter.dto.CreateUserRequest;
import com.example.expensesplitter.dto.UserResponse;
import com.example.expensesplitter.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The controller only deals with HTTP: read the request, call the service, return the result.
 * @RestController = @Controller + @ResponseBody: return values are written as JSON.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** POST /users -> 201 Created. @Valid triggers the validation annotations on the DTO. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    /** GET /users -> 200 OK */
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    /** GET /users/{id} -> 200 OK, or 404 if not found */
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable("id") Long id) {
        return userService.getUser(id);
    }
}
