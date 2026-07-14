package com.example.backend.repository;

import com.example.backend.model.CallSession;
import com.example.backend.model.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, Long> {
    List<CallSession> findByReceiverIdAndStatus(Long receiverId, CallStatus status);
    Optional<CallSession> findTopByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, CallStatus status);
    List<CallSession> findByCallerIdAndStatus(Long callerId, CallStatus status);
}
