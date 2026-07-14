package com.example.backend.repository;

import com.example.backend.model.PropertyVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PropertyVerificationRepository extends JpaRepository<PropertyVerification, Long> {
    List<PropertyVerification> findByPropertyId(Long propertyId);
}
