package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OcrVerificationService {

    private final TextractClient textractClient;

    /**
     * Parses an uploaded utility bill image/PDF using AWS Textract,
     * and validates if it contains the landlord's name and property address.
     *
     * @param billFile The utility bill MultipartFile (JPEG, PNG, or PDF).
     * @param expectedName The expected name of the landlord (e.g., "John Doe").
     * @param expectedAddress The expected physical address of the listing.
     * @return OcrMatchResult containing success status, trust score, and log summary.
     */
    public OcrMatchResult verifyUtilityBill(MultipartFile billFile, String expectedName, String expectedAddress) {
        try {
            // 1. Prepare raw bytes for Textract
            SdkBytes sdkBytes = SdkBytes.fromInputStream(billFile.getInputStream());
            Document document = Document.builder().bytes(sdkBytes).build();

            DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                    .document(document)
                    .build();

            // 2. Call Textract API
            DetectDocumentTextResponse response = textractClient.detectDocumentText(request);
            
            // 3. Aggregate all extracted text lines
            List<String> textLines = response.blocks().stream()
                    .filter(block -> block.blockType() == BlockType.LINE)
                    .map(Block::text)
                    .collect(Collectors.toList());

            String fullExtractedText = textLines.stream()
                    .collect(Collectors.joining(" "))
                    .toLowerCase();

            // 4. Perform Name Verification Check
            boolean nameMatched = checkNameMatch(fullExtractedText, expectedName);

            // 5. Perform Address Verification Check
            boolean addressMatched = checkAddressMatch(fullExtractedText, expectedAddress);

            double score = 0.0;
            if (nameMatched) score += 50.0;
            if (addressMatched) score += 50.0;

            String summaryMessage = String.format(
                "Utility Bill OCR Audit completed. Name match: %b. Address match: %b. Trust Score: %.1f%%",
                nameMatched, addressMatched, score
            );

            return new OcrMatchResult(nameMatched && addressMatched, score, summaryMessage, fullExtractedText);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read utility bill file bytes for OCR analysis", e);
        } catch (Exception e) {
            throw new RuntimeException("Error processing Amazon Textract OCR document pipeline", e);
        }
    }

    private boolean checkNameMatch(String billText, String expectedName) {
        if (expectedName == null || expectedName.trim().isEmpty()) {
            return false;
        }
        
        String cleanExpected = expectedName.toLowerCase().trim();
        if (billText.contains(cleanExpected)) {
            return true;
        }

        String[] parts = cleanExpected.split("\\s+");
        for (String part : parts) {
            if (part.length() > 2 && !billText.contains(part)) {
                return false; // All significant name segments must appear in the utility bill
            }
        }
        return parts.length > 0;
    }

    private boolean checkAddressMatch(String billText, String expectedAddress) {
        if (expectedAddress == null || expectedAddress.trim().isEmpty()) {
            return false;
        }

        String cleanExpected = expectedAddress.toLowerCase().trim();
        if (billText.contains(cleanExpected)) {
            return true;
        }

        // Address fallback matching (streets, numbers, city tokens)
        String[] segments = cleanExpected.replaceAll("[,\\.\\-]", " ").split("\\s+");
        int matches = 0;
        int matchableSegments = 0;

        for (String segment : segments) {
            if (segment.length() > 2) { // Only match words longer than 2 characters
                matchableSegments++;
                if (billText.contains(segment)) {
                    matches++;
                }
            }
        }

        if (matchableSegments == 0) return false;
        
        // Require at least 60% of the address keywords to match to account for abbreviations
        double matchRatio = (double) matches / matchableSegments;
        return matchRatio >= 0.60;
    }

    public static class OcrMatchResult {
        private final boolean success;
        private final double score;
        private final String message;
        private final String extractedText;

        public OcrMatchResult(boolean success, double score, String message, String extractedText) {
            this.success = success;
            this.score = score;
            this.message = message;
            this.extractedText = extractedText;
        }

        public boolean isSuccess() { return success; }
        public double getScore() { return score; }
        public String getMessage() { return message; }
        public String getExtractedText() { return extractedText; }
    }
}
