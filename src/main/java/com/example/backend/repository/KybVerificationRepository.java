package com.example.backend.repository;

import com.example.backend.model.KybVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KybVerificationRepository extends JpaRepository<KybVerification, Long> {
    Optional<KybVerification> findByUserId(Long userId);
}
