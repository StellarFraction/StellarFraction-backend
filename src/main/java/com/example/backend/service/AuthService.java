package com.example.backend.service;

import com.example.backend.dto.request.ForgotPasswordRequest;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.request.ResetPasswordRequest;
import com.example.backend.dto.response.AuthResponse;

public interface AuthService {
    boolean registerUser(RegisterRequest dto, String deviceFingerprint, String deviceDetails, String ipAddress);
    AuthResponse loginUser(LoginRequest dto, String deviceFingerprint, String deviceDetails, String ipAddress);
    void forgotPassword(ForgotPasswordRequest dto);
    void resetPassword(ResetPasswordRequest dto);
    void verifyEmail(String email, String token);
    void logout(String token);
    void changePassword(String email, com.example.backend.dto.request.ChangePasswordRequest dto);
    void deleteAccount(String email, String token);
}