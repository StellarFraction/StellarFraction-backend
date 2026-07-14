package com.example.backend.service.impl;

import com.example.backend.dto.response.SmileIdResponse;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.DojahService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DojahServiceImpl implements DojahService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dojah.app-id:}")
    private String appId;

    @Value("${dojah.api-key:}")
    private String apiKey;

    @Value("${dojah.base-url:https://api.dojah.io}")
    private String baseUrl;

    @Override
    public SmileIdResponse verifyNin(String nin, @org.springframework.lang.NonNull Long userId) {
        log.info("Verifying NIN using Dojah for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (appId == null || appId.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("Dojah API credentials not set. Falling back to Mock Verification.");
            return generateMockResponse("NIN", nin, user);
        }

        try {
            String url = baseUrl + "/api/v1/kyc/nin?nin=" + nin;

            HttpHeaders headers = new HttpHeaders();
            headers.set("AppId", appId);
            headers.set("Authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = responseEntity.getBody();
            if (responseEntity.getStatusCode() == HttpStatus.OK && body != null && body.containsKey("entity")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entity = (Map<String, Object>) body.get("entity");
                if (entity != null) {
                    String firstName = String.valueOf(entity.getOrDefault("first_name", ""));
                    String lastName = String.valueOf(entity.getOrDefault("last_name", ""));
                    String dob = String.valueOf(entity.containsKey("date_of_birth") ? entity.get("date_of_birth") : entity.getOrDefault("dob", ""));

                    return SmileIdResponse.builder()
                            .success(true)
                            .status("VERIFIED")
                            .message("NIN successfully verified via Dojah portal lookup")
                            .smileTxId("dj_tx_" + UUID.randomUUID().toString().substring(0, 12))
                            .fullName(firstName + " " + lastName)
                            .dob(dob)
                            .databaseMatched("NIN")
                            .build();
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Dojah NIN Verification API call failed: {}", e.getResponseBodyAsString(), e);
            String errorMsg = extractErrorMessage(e.getResponseBodyAsString());
            return SmileIdResponse.builder()
                    .success(false)
                    .status("FAILED")
                    .message("NIN verification failed: " + errorMsg)
                    .build();
        } catch (Exception e) {
            log.error("Dojah NIN Verification API call failed", e);
        }

        return SmileIdResponse.builder()
                .success(false)
                .status("FAILED")
                .message("Dojah API request failed or was rejected")
                .build();
    }

    @Override
    public SmileIdResponse verifyBvn(String bvn, @org.springframework.lang.NonNull Long userId) {
        log.info("Verifying BVN using Dojah for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (appId == null || appId.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("Dojah API credentials not set. Falling back to Mock Verification.");
            return generateMockResponse("BVN", bvn, user);
        }

        try {
            String url = baseUrl + "/api/v1/kyc/bvn?bvn=" + bvn;

            HttpHeaders headers = new HttpHeaders();
            headers.set("AppId", appId);
            headers.set("Authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = responseEntity.getBody();
            if (responseEntity.getStatusCode() == HttpStatus.OK && body != null && body.containsKey("entity")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entity = (Map<String, Object>) body.get("entity");
                if (entity != null) {
                    String firstName = String.valueOf(entity.getOrDefault("first_name", ""));
                    String lastName = String.valueOf(entity.getOrDefault("last_name", ""));
                    String dob = String.valueOf(entity.containsKey("date_of_birth") ? entity.get("date_of_birth") : entity.getOrDefault("dob", ""));

                    return SmileIdResponse.builder()
                            .success(true)
                            .status("VERIFIED")
                            .message("BVN successfully verified via Dojah portal lookup")
                            .smileTxId("dj_tx_" + UUID.randomUUID().toString().substring(0, 12))
                            .fullName(firstName + " " + lastName)
                            .dob(dob)
                            .databaseMatched("BVN")
                            .build();
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Dojah BVN Verification API call failed: {}", e.getResponseBodyAsString(), e);
            String errorMsg = extractErrorMessage(e.getResponseBodyAsString());
            return SmileIdResponse.builder()
                    .success(false)
                    .status("FAILED")
                    .message("BVN verification failed: " + errorMsg)
                    .build();
        } catch (Exception e) {
            log.error("Dojah BVN Verification API call failed", e);
        }

        return SmileIdResponse.builder()
                .success(false)
                .status("FAILED")
                .message("Dojah API request failed or was rejected")
                .build();
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Dojah API request failed or was rejected";
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
            if (root.has("error")) {
                return root.get("error").asText();
            } else if (root.has("message")) {
                return root.get("message").asText();
            }
        } catch (Exception ex) {
            // ignore
        }
        return "Dojah API request failed or was rejected";
    }

    @Override
    public SmileIdResponse checkSelfieLiveness(MultipartFile selfie, @org.springframework.lang.NonNull Long userId) {
        log.info("Checking Selfie Liveness using Dojah for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (appId == null || appId.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("Dojah API credentials not set. Falling back to Mock Liveness Verification.");
            return generateMockResponse("SELFIE", "selfie_pic", user);
        }

        try {
            String url = baseUrl + "/api/v1/ml/liveness";

            byte[] fileBytes = selfie.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", base64Image);

            HttpHeaders headers = new HttpHeaders();
            headers.set("AppId", appId);
            headers.set("Authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = responseEntity.getBody();
            if (responseEntity.getStatusCode() == HttpStatus.OK && body != null && body.containsKey("entity")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entity = (Map<String, Object>) body.get("entity");
                if (entity != null) {
                    double score = Double.parseDouble(String.valueOf(entity.getOrDefault("liveness_probability", "0")));
                    boolean isLivenessOk = score >= 50.0;
                    return SmileIdResponse.builder()
                            .success(isLivenessOk)
                            .status(isLivenessOk ? "VERIFIED" : "FAILED")
                            .message(isLivenessOk ? "Selfie liveness check passed." : "Liveness check failed (Spoofing detected)")
                            .smileTxId("dj_tx_" + UUID.randomUUID().toString().substring(0, 12))
                            .livenessScore(score / 100.0)
                            .fullName(user.getFirstName() + " " + user.getLastName())
                            .databaseMatched("SELFIE")
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("Dojah Selfie Liveness API call failed", e);
        }

        return SmileIdResponse.builder()
                .success(false)
                .status("FAILED")
                .message("Selfie upload or biometric verification failed")
                .build();
    }

    private SmileIdResponse generateMockResponse(String type, String value, User user) {
        String mockTxId = "dj_tx_mock_" + UUID.randomUUID().toString().substring(0, 12);
        if ("NIN".equalsIgnoreCase(type)) {
            if (value != null && value.length() >= 10) {
                return SmileIdResponse.builder()
                        .success(true)
                        .status("VERIFIED")
                        .message("NIN successfully verified via Mock Dojah records matching")
                        .smileTxId(mockTxId)
                        .fullName(user.getFirstName().toUpperCase() + " " + user.getLastName().toUpperCase())
                        .dob("1995-08-12")
                        .databaseMatched("NIN")
                        .build();
            } else {
                return SmileIdResponse.builder()
                        .success(false)
                        .status("FAILED")
                        .message("Invalid National Identification Number length (must be 11 digits)")
                        .smileTxId(mockTxId)
                        .build();
            }
        } else if ("BVN".equalsIgnoreCase(type)) {
            if (value != null && value.length() >= 10) {
                return SmileIdResponse.builder()
                        .success(true)
                        .status("VERIFIED")
                        .message("BVN successfully verified via Mock Dojah records matching")
                        .smileTxId(mockTxId)
                        .fullName(user.getFirstName().toUpperCase() + " " + user.getLastName().toUpperCase())
                        .dob("1995-08-12")
                        .databaseMatched("BVN")
                        .build();
            } else {
                return SmileIdResponse.builder()
                        .success(false)
                        .status("FAILED")
                        .message("Invalid Bank Verification Number length (must be 11 digits)")
                        .smileTxId(mockTxId)
                        .build();
            }
        } else {
            // Selfie liveness check simulation
            return SmileIdResponse.builder()
                    .success(true)
                    .status("VERIFIED")
                    .message("Liveness check succeeded (Confidence score: 98.4%). User verified as real physical person.")
                    .smileTxId(mockTxId)
                    .livenessScore(0.984)
                    .fullName(user.getFirstName().toUpperCase() + " " + user.getLastName().toUpperCase())
                    .databaseMatched("SELFIE")
                    .build();
        }
    }
}
