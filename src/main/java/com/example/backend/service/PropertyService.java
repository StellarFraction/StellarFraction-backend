package com.example.backend.service;

import com.example.backend.dto.response.PortfolioResponse;
import com.example.backend.model.Property;
import java.util.List;

public interface PropertyService {
    Property createProperty(@org.springframework.lang.NonNull Property property, @org.springframework.lang.NonNull Long sellerId);
    List<Property> getPropertiesForSale();
    List<Property> getPropertiesForRent();
    List<Property> getAllProperties();
    PortfolioResponse getUserPortfolio(@org.springframework.lang.NonNull Long userId);
    Property getPropertyById(@org.springframework.lang.NonNull Long id);
    Property updateProperty(@org.springframework.lang.NonNull Long id, @org.springframework.lang.NonNull Property propertyDetails, @org.springframework.lang.NonNull Long sellerId);
}