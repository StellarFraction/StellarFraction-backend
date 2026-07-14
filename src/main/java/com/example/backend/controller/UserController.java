package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getMyDetails() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        return ResponseEntity.ok(ApiResponse.success("Authenticated user details retrieved successfully", authenticatedUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserDetails(@PathVariable("id") Long id) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        if (!authenticatedUser.getId().equals(id)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You can only retrieve your own user details."));
        }

        return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", authenticatedUser));
    }
}
