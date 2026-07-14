package com.example.backend.service.impl;

import com.example.backend.dto.request.BookingRequest;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Booking;
import com.example.backend.model.BookingStatus;
import com.example.backend.model.Property;
import com.example.backend.model.PropertyStatus;
import com.example.backend.model.User;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.PropertyRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Override
    public Booking createBooking(@org.springframework.lang.NonNull BookingRequest request, @org.springframework.lang.NonNull Long buyerId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        Property property = propertyRepository.findById(java.util.Objects.requireNonNull(request.getPropertyId(), "Property ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (property.getStatus() != PropertyStatus.FOR_RENT) {
            throw new ConflictException("This property is not available for rent");
        }

        if (!property.isVerified()) {
            throw new ConflictException("This property has not been verified yet");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (days <= 0) {
            throw new ConflictException("End date must be after start date");
        }

        BigDecimal totalPrice = property.getPrice().multiply(BigDecimal.valueOf(days));

        Booking booking = new Booking();
        booking.setBuyer(buyer);
        booking.setProperty(property);
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }
}
