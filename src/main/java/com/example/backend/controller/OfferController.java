package com.example.backend.controller;

import com.example.backend.dto.request.OfferRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.model.Offer;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Offer>> createOffer(@Valid @RequestBody @org.springframework.lang.NonNull OfferRequest request, @RequestParam @org.springframework.lang.NonNull Long buyerId) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        if (!authenticatedUser.getId().equals(buyerId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You cannot create an offer on behalf of another user."));
        }
        if (!authenticatedUser.isIdentityVerified()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You must verify your identity via Dojah first to submit offers."));
        }
        Offer offer = offerService.createOffer(request, buyerId);
        return ResponseEntity.ok(ApiResponse.success("Offer submitted successfully", offer));
    }
}
