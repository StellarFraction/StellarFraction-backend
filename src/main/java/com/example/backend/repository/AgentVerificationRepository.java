package com.example.backend.repository;

import com.example.backend.model.AgentVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentVerificationRepository extends JpaRepository<AgentVerification, Long> {
    Optional<AgentVerification> findByUserId(Long userId);
}
