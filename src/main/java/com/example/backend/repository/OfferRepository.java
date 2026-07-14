package com.example.backend.repository;

import com.example.backend.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    List<Offer> findByBuyerId(Long buyerId);
    List<Offer> findByPropertyId(Long propertyId);
}
