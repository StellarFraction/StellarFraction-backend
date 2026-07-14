package com.example.backend.service;

import com.example.backend.dto.request.ReviewRequest;
import com.example.backend.model.Review;

public interface ReviewService {
    Review createReview(@org.springframework.lang.NonNull ReviewRequest request, @org.springframework.lang.NonNull Long reviewerId);
}
