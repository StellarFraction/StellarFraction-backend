package com.example.backend.service.impl;

import com.example.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ResendEmailServiceImpl implements EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Override
    public void sendPasswordResetEmail(String to, String resetToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(java.util.Objects.requireNonNull(apiKey, "Resend API key must not be null"));

        Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", new String[]{to},
                "subject", "Your Keyz Password Reset Token",
                "html", "<p>Use the following token to reset your password:</p><h2>" + resetToken + "</h2><p>This token expires in 15 minutes.</p>"
        );

        System.out.println("=================================================");
        System.out.println("🔑 PASSWORD RESET TOKEN GENERATED FOR: " + to);
        System.out.println("TOKEN: " + resetToken);
        System.out.println("=================================================");

        try {
            restTemplate.postForEntity(RESEND_API_URL, new HttpEntity<>(body, headers), String.class);
            System.out.println("📧 Reset email successfully dispatched via Resend.");
        } catch (Exception ex) {
            System.err.println("❌ ERROR: Failed to send password reset email via Resend API: " + ex.getMessage());
            System.err.println("💡 TIP: Make sure your RESEND_API_KEY is valid and the sending email '" + fromEmail + "' is authorized/verified.");
        }
    }

    @Override
    public void sendEmailVerificationOtp(String to, String otp) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(java.util.Objects.requireNonNull(apiKey, "Resend API key must not be null"));

        Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", new String[]{to},
                "subject", "Verify your Keyz Email Address",
                "html", "<p>Welcome to Keyz! Please use the following 6-digit OTP code to verify your email address:</p><h2>" + otp + "</h2><p>This code is valid for 24 hours.</p>"
        );

        System.out.println("=================================================");
        System.out.println("✉️ EMAIL VERIFICATION OTP GENERATED FOR: " + to);
        System.out.println("OTP CODE: " + otp);
        System.out.println("=================================================");

        try {
            restTemplate.postForEntity(RESEND_API_URL, new HttpEntity<>(body, headers), String.class);
            System.out.println("📧 Verification email successfully dispatched via Resend.");
        } catch (Exception ex) {
            System.err.println("❌ ERROR: Failed to send email verification OTP via Resend API: " + ex.getMessage());
            System.err.println("💡 TIP: Make sure your RESEND_API_KEY is valid and the sending email '" + fromEmail + "' is authorized/verified.");
        }
    }
}
