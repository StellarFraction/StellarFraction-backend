package com.example.backend.service;

import com.example.backend.dto.response.SmileIdResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DojahService {
    
    /**
     * Verifies a user's National Identification Number (NIN) against the government/Dojah database.
     */
    SmileIdResponse verifyNin(String nin, @org.springframework.lang.NonNull Long userId);

    /**
     * Verifies a user's Bank Verification Number (BVN) against the government/Dojah database.
     */
    SmileIdResponse verifyBvn(String bvn, @org.springframework.lang.NonNull Long userId);

    /**
     * Performs a selfie liveness check and biometric comparison.
     */
    SmileIdResponse checkSelfieLiveness(MultipartFile selfie, @org.springframework.lang.NonNull Long userId);
}
