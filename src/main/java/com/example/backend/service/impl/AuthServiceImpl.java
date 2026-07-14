package com.example.backend.service.impl;

import com.example.backend.dto.request.ForgotPasswordRequest;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.request.ResetPasswordRequest;
import com.example.backend.dto.request.ChangePasswordRequest;
import com.example.backend.dto.response.AuthResponse;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.model.DeviceFingerprint;
import com.example.backend.model.EmailVerificationToken;
import com.example.backend.model.PasswordResetToken;
import com.example.backend.model.User;
import com.example.backend.repository.DeviceFingerprintRepository;
import com.example.backend.repository.PasswordResetTokenRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.EmailVerificationTokenRepository;
import com.example.backend.security.JwtService;
import com.example.backend.service.AuthService;
import com.example.backend.service.EmailService;
import com.example.backend.model.BlacklistedToken;
import com.example.backend.repository.BlacklistedTokenRepository;
import com.example.backend.model.DeletedAccount;
import com.example.backend.repository.DeletedAccountRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository tokenRepository;
    private final DeviceFingerprintRepository fingerprintRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepo;
    private final EmailService emailService;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final DeletedAccountRepository deletedAccountRepository;
    private final EntityManager entityManager;

    @Override
    public boolean registerUser(RegisterRequest dto, String fingerprintHash, String deviceDetails, String ipAddress) {
        String finalFingerprint = (fingerprintHash != null && !fingerprintHash.isEmpty()) 
                ? fingerprintHash 
                : generateFallbackFingerprint(ipAddress, deviceDetails);

        String email = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : "";

        if (userRepository.findByEmail(email).isPresent()) {
            return false;
        }
        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setEmailVerified(false); // Force pending verification
        User savedUser = userRepository.save(user);
        
        // Always save a fingerprint (either provided or fallback)
        DeviceFingerprint fp = new DeviceFingerprint();
        fp.setUser(savedUser);
        fp.setFingerprintHash(finalFingerprint);
        fp.setDeviceDetails(deviceDetails != null ? deviceDetails : "Unknown Device");
        fp.setLastSeenAt(LocalDateTime.now());
        fingerprintRepository.save(fp);

        // Generate 6-digit email verification OTP code
        String emailOtp = String.format("%06d", (int)(Math.random() * 900000) + 100000);
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(emailOtp);
        verificationToken.setUser(savedUser);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24)); // Valid for 24 hours
        emailVerificationTokenRepo.save(verificationToken);

        // Send OTP email
        emailService.sendEmailVerificationOtp(savedUser.getEmail(), emailOtp);
        return true;
    }

    @Override
    public AuthResponse loginUser(LoginRequest dto, String fingerprintHash, String deviceDetails, String ipAddress) {
        String finalFingerprint = (fingerprintHash != null && !fingerprintHash.isEmpty()) 
                ? fingerprintHash 
                : generateFallbackFingerprint(ipAddress, deviceDetails);

        String email = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : "";

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Please verify your email address before logging in.");
        }

        if (finalFingerprint != null) {
            DeviceFingerprint fp = fingerprintRepository.findByUserIdAndFingerprintHash(user.getId(), finalFingerprint)
                    .orElseGet(() -> {
                        DeviceFingerprint newFp = new DeviceFingerprint();
                        newFp.setUser(user);
                        newFp.setFingerprintHash(finalFingerprint);
                        newFp.setDeviceDetails(deviceDetails != null ? deviceDetails : "Unknown Device");
                        return newFp;
                    });
            fp.setLastSeenAt(LocalDateTime.now());
            fingerprintRepository.save(fp);
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                Map.of("role", user.getRole().name())
        );

        return AuthResponse.builder()
                .accessToken(token)
                .role(user.getRole().name())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest dto) {
        String email = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : "";

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with that email address"));

        String token = String.format("%06d", (int)(Math.random() * 900000) + 100000);
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15)); // Reduced expiry to 15 mins for stronger OTP security
        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Override
    public void resetPassword(ResetPasswordRequest dto) {
        PasswordResetToken resetToken = tokenRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            throw new UnauthorizedException("This reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        tokenRepository.delete(resetToken);
    }

    @Override
    public void verifyEmail(String email, String token) {
        String lowerEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userRepository.findByEmail(lowerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with that email address"));

        EmailVerificationToken verificationToken = emailVerificationTokenRepo.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired email verification token"));

        if (!verificationToken.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Invalid email verification token");
        }

        if (verificationToken.isExpired()) {
            throw new UnauthorizedException("This email verification token has expired");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationTokenRepo.delete(verificationToken);
    }

    @Override
    public void logout(String token) {
        try {
            String jti = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getId);
            java.util.Date expiration = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getExpiration);
            
            if (jti != null) {
                BlacklistedToken blacklisted = new BlacklistedToken();
                blacklisted.setTokenJti(jti);
                
                java.time.LocalDateTime expiry = java.time.LocalDateTime.now().plusHours(24);
                if (expiration != null) {
                    expiry = expiration.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                }
                blacklisted.setExpiryTime(expiry);
                blacklistedTokenRepository.save(blacklisted);
            }
        } catch (Exception e) {
            // Token is already invalid or expired, ignore
        }
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new UnauthorizedException("Incorrect old password");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteAccount(String email, String token) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long userId = user.getId();

        // 1. Save user details in deleted_accounts table
        DeletedAccount deletedAccount = DeletedAccount.builder()
                .originalUserId(userId)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .deletedAt(LocalDateTime.now())
                .build();
        deletedAccountRepository.save(deletedAccount);

        // 2. Cascade delete records programmatically to avoid FK constraint violations
        java.util.List<Long> propertyIds = entityManager.createQuery(
                "SELECT p.id FROM Property p WHERE p.seller.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getResultList();

        if (!propertyIds.isEmpty()) {
            entityManager.createQuery("DELETE FROM PropertyVerification pv WHERE pv.property.id IN :propertyIds")
                .setParameter("propertyIds", propertyIds)
                .executeUpdate();

            entityManager.createQuery("DELETE FROM Offer o WHERE o.property.id IN :propertyIds")
                .setParameter("propertyIds", propertyIds)
                .executeUpdate();

            entityManager.createQuery("DELETE FROM Booking b WHERE b.property.id IN :propertyIds")
                .setParameter("propertyIds", propertyIds)
                .executeUpdate();

            entityManager.createQuery("DELETE FROM Review r WHERE r.property.id IN :propertyIds")
                .setParameter("propertyIds", propertyIds)
                .executeUpdate();

            entityManager.createQuery("DELETE FROM Property p WHERE p.id IN :propertyIds")
                .setParameter("propertyIds", propertyIds)
                .executeUpdate();
        }

        entityManager.createQuery("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        entityManager.createQuery("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        entityManager.createQuery("DELETE FROM DeviceFingerprint f WHERE f.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        entityManager.createQuery("DELETE FROM AgentVerification av WHERE av.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        entityManager.createQuery("DELETE FROM KybVerification kv WHERE kv.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        entityManager.createQuery("DELETE FROM PayoutVerification pv WHERE pv.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        entityManager.createQuery("DELETE FROM ChatMessage cm WHERE cm.sender.id = :userId OR cm.receiver.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        entityManager.createQuery("DELETE FROM CallSession cs WHERE cs.caller.id = :userId OR cs.receiver.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        entityManager.createQuery("DELETE FROM Offer o WHERE o.buyer.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        entityManager.createQuery("DELETE FROM Booking b WHERE b.buyer.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        entityManager.createQuery("DELETE FROM Review r WHERE r.reviewer.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        // 3. Delete user
        entityManager.createQuery("DELETE FROM User u WHERE u.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();

        // 4. Blacklist / invalidate current session token
        logout(token);
    }

    private String generateFallbackFingerprint(String ipAddress, String deviceDetails) {
        try {
            String raw = (ipAddress != null ? ipAddress : "unknown-ip") + (deviceDetails != null ? deviceDetails : "unknown-device");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString(); // Last resort fallback
        }
    }
}
