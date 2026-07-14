package com.example.backend.repository;

import com.example.backend.model.Property;
import com.example.backend.model.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.math.BigDecimal;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findBySellerId(Long sellerId);
    List<Property> findByPriceLessThan(BigDecimal maxPrice);
    List<Property> findByStatusAndIsVerifiedTrue(PropertyStatus status);
    List<Property> findByImageHashIsNotNull();
}