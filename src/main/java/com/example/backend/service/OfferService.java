package com.example.backend.service;

import com.example.backend.dto.request.OfferRequest;
import com.example.backend.model.Offer;

public interface OfferService {
    Offer createOffer(@org.springframework.lang.NonNull OfferRequest request, @org.springframework.lang.NonNull Long buyerId);
}
