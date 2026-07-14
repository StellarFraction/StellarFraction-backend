package com.example.backend.service;

import com.example.backend.dto.request.BookingRequest;
import com.example.backend.model.Booking;

public interface BookingService {
    Booking createBooking(@org.springframework.lang.NonNull BookingRequest request, @org.springframework.lang.NonNull Long buyerId);
}
