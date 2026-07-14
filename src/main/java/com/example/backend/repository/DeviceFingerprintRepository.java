package com.example.backend.repository;

import com.example.backend.model.DeviceFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviceFingerprintRepository extends JpaRepository<DeviceFingerprint, Long> {
    Optional<DeviceFingerprint> findByUserIdAndFingerprintHash(Long userId, String fingerprintHash);
}
