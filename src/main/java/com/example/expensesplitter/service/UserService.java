package com.example.expensesplitter.service;

import com.example.expensesplitter.dto.CreateUserRequest;
import com.example.expensesplitter.dto.UserResponse;
import com.example.expensesplitter.entity.User;
import com.example.expensesplitter.exception.ConflictException;
import com.example.expensesplitter.exception.ResourceNotFoundException;
import com.example.expensesplitter.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for users. The controller calls this class; this class calls the repository.
 * Keeping rules here (not in the controller) keeps the controller thin and easy to read.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    // Constructor injection: Spring passes in the repository it created.
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** @Transactional: everything in the method succeeds together or is rolled back together. */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(); // "A@x.com" and "a@x.com" are the same person
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("A user with email '" + email + "' already exists");
        }
        User saved = userRepository.save(new User(request.name().trim(), email));
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return UserResponse.from(getUserEntity(id));
    }

    /** Used by other services that need the entity itself. Throws 404 if the user doesn't exist. */
    @Transactional(readOnly = true)
    public User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }
}
