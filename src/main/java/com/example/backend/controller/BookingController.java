package com.example.backend.controller;

import com.example.backend.dto.request.BookingRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.model.Booking;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(@Valid @RequestBody @org.springframework.lang.NonNull BookingRequest request, @RequestParam @org.springframework.lang.NonNull Long buyerId) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        if (!authenticatedUser.getId().equals(buyerId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You cannot create a booking on behalf of another user."));
        }
        if (!authenticatedUser.isIdentityVerified()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You must verify your identity via Dojah first to book properties."));
        }
        Booking booking = bookingService.createBooking(request, buyerId);
        return ResponseEntity.ok(ApiResponse.success("Booking created successfully", booking));
    }
}
