package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.CallSession;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtService;
import com.example.backend.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<CallSession>> initiateCall(
            @RequestParam("receiverId") Long receiverId,
            @RequestParam("propertyId") Long propertyId,
            @RequestHeader("Authorization") String authHeader) {

        User caller = extractUser(authHeader);
        CallSession session = callService.initiateCall(caller.getId(), receiverId, propertyId);
        return ResponseEntity.ok(ApiResponse.success("Call initiated successfully. Ringing receiver...", session));
    }

    @PostMapping("/{callId}/accept")
    public ResponseEntity<ApiResponse<CallSession>> acceptCall(
            @PathVariable("callId") Long callId,
            @RequestHeader("Authorization") String authHeader) {

        User receiver = extractUser(authHeader);
        CallSession session = callService.acceptCall(callId, receiver.getId());
        return ResponseEntity.ok(ApiResponse.success("Call accepted. Connecting room session...", session));
    }

    @PostMapping("/{callId}/reject")
    public ResponseEntity<ApiResponse<CallSession>> rejectCall(
            @PathVariable("callId") Long callId,
            @RequestHeader("Authorization") String authHeader) {

        User receiver = extractUser(authHeader);
        CallSession session = callService.rejectCall(callId, receiver.getId());
        return ResponseEntity.ok(ApiResponse.success("Call rejected successfully", session));
    }

    @PostMapping("/{callId}/end")
    public ResponseEntity<ApiResponse<CallSession>> endCall(
            @PathVariable("callId") Long callId,
            @RequestHeader("Authorization") String authHeader) {

        User user = extractUser(authHeader);
        CallSession session = callService.endCall(callId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Call session ended successfully", session));
    }

    @GetMapping("/incoming")
    public ResponseEntity<ApiResponse<CallSession>> getIncomingCall(
            @RequestHeader("Authorization") String authHeader) {

        User receiver = extractUser(authHeader);
        Optional<CallSession> incoming = callService.getIncomingCall(receiver.getId());
        if (incoming.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("Incoming call detected", incoming.get()));
        }
        return ResponseEntity.ok(ApiResponse.success("No pending incoming calls found", null));
    }

    @GetMapping("/{callId}/status")
    public ResponseEntity<ApiResponse<CallSession>> getCallStatus(
            @PathVariable("callId") Long callId,
            @RequestHeader("Authorization") @SuppressWarnings("unused") String authHeader) {

        // Validate auth token presence
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        CallSession session = callService.getCallStatus(callId);
        return ResponseEntity.ok(ApiResponse.success("Call session status retrieved", session));
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
