package com.example.backend.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String resetToken);
    void sendEmailVerificationOtp(String to, String otp);
}
