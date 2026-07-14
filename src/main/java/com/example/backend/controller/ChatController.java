package com.example.backend.controller;

import com.example.backend.dto.request.ChatMessageRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ChatMessageResponse;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtService;
import com.example.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            @RequestHeader("Authorization") String authHeader) {

        User user = extractUser(authHeader);
        ChatMessageResponse response = chatService.sendMessage(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Message dispatched in transit queue successfully", response));
    }

    @GetMapping("/receive")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> receivePendingMessages(
            @RequestHeader("Authorization") String authHeader) {

        User user = extractUser(authHeader);
        List<ChatMessageResponse> messages = chatService.retrievePendingMessages(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Pending messages consumed and cleared from server", messages));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Long>> getPendingCount(
            @RequestHeader("Authorization") String authHeader) {

        User user = extractUser(authHeader);
        long count = chatService.getPendingCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Pending offline messages count retrieved successfully", count));
    }

    private User extractUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
