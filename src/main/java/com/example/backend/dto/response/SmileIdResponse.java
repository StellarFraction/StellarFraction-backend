package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmileIdResponse {
    private boolean success;
    private String status; // "VERIFIED", "FAILED", "PENDING"
    private String message;
    private String smileTxId;
    private Double livenessScore;
    private String fullName;
    private String dob;
    private String databaseMatched; // "NIN", "BVN", "SELFIE"
}
