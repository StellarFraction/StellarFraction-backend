package com.example.backend.repository;

import com.example.backend.model.PayoutVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PayoutVerificationRepository extends JpaRepository<PayoutVerification, Long> {
    List<PayoutVerification> findByUserId(Long userId);
    Optional<PayoutVerification> findByUserIdAndIsVerifiedTrue(Long userId);
}
