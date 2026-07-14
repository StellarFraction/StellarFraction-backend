package com.example.backend.controller;

import com.example.backend.dto.request.ReviewRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.model.Review;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Review>> createReview(@Valid @RequestBody @org.springframework.lang.NonNull ReviewRequest request, @RequestParam @org.springframework.lang.NonNull Long reviewerId) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        if (!authenticatedUser.getId().equals(reviewerId)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You cannot submit a review on behalf of another user."));
        }
        Review review = reviewService.createReview(request, reviewerId);
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully", review));
    }
}
