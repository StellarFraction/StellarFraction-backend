package com.example.backend.service.impl;

import com.example.backend.dto.request.ReviewRequest;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Booking;
import com.example.backend.model.BookingStatus;
import com.example.backend.model.Property;
import com.example.backend.model.Review;
import com.example.backend.model.User;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.PropertyRepository;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ReviewService;
import com.example.backend.util.DisintermediationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final DisintermediationFilter disintermediationFilter;

    @Override
    public Review createReview(@org.springframework.lang.NonNull ReviewRequest request, @org.springframework.lang.NonNull Long reviewerId) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        Property property = propertyRepository.findById(java.util.Objects.requireNonNull(request.getPropertyId(), "Property ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        List<Booking> userBookings = bookingRepository.findByBuyerId(reviewerId);
        boolean hasCompletedBooking = userBookings.stream()
                .anyMatch(b -> b.getProperty().getId().equals(property.getId())
                            && b.getStatus() == BookingStatus.COMPLETED);

        if (!hasCompletedBooking) {
            throw new ForbiddenException("You can only review properties you have completed a rental for");
        }

        Review review = new Review();
        review.setProperty(property);
        review.setReviewer(reviewer);
        review.setRating(request.getRating());
        
        // Sanitize review comments
        review.setComment(disintermediationFilter.sanitize(request.getComment()));
        reviewRepository.save(review);

        // Update seller aggregate rating
        User seller = property.getSeller();
        List<Review> sellerReviews = reviewRepository.findByPropertySellerId(seller.getId());
        double average = sellerReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        
        seller.setSellerRating(average);
        userRepository.save(seller);

        return review;
    }
}
