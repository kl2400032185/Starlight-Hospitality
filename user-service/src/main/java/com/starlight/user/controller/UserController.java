package com.starlight.user.controller;

import com.starlight.user.entity.User;
import com.starlight.user.service.UserService;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable Long id
    ) {

        User user =
                userService.getUserById(id);

        UserResponse response =
                new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getCreatedAt()
                );

        return ResponseEntity.ok(response);
    }

    public record UserResponse(
            Long id,
            String name,
            String email,
            String role,
            LocalDateTime createdAt
    ) {
    }
}