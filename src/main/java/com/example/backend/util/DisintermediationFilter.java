package com.example.backend.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Component
public class DisintermediationFilter {

    // Regex for Email detection
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}"
    );

    // Regex for URL/Link detection
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(https?://|www\\.)[a-z0-9.-]+\\.[a-z]{2,6}(/\\S*)?"
    );

    // Pattern to detect international phone numbers (starts with + or 00, followed by 9-15 digits with optional spacing/punctuation)
    private static final Pattern INT_PHONE_PATTERN = Pattern.compile(
            "(?i)(?:\\+|00)(?:\\s*\\d\\s*[-()\\.]*\\s*){9,15}"
    );

    // Pattern to detect local phone numbers starting with 0 (e.g. 08031234567, 070... etc) with optional formatting
    // Typically 10 to 11 digits
    private static final Pattern LOCAL_PHONE_PATTERN = Pattern.compile(
            "(?i)\\b0(?:\\s*\\d\\s*[-()\\.]*\\s*){9,10}\\b"
    );

    // Platform-bypass keywords
    private static final String[] BYPASS_KEYWORDS = {
            "whatsapp", "telegram", "instagram", "snapchat", "gmail", "outlook", "yahoo",
            "zelle", "paypal", "venmo", "cashapp", "wire transfer", "bank transfer",
            "pay directly", "pay outside", "outside the app", "avoid fees", "bypass the app",
            "dm me", "direct message", "call me at", "contact me at", "my number is",
            "phone number", "cell number", "mobile number"
    };

    /**
     * Sanitizes input text by redacting emails, website links, phone numbers, and platform bypass terms.
     * Keeps the structure intact but replaces sensitive bypass info with appropriate placeholders.
     */
    public String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String sanitized = input;

        // 1. Redact Emails
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("[REDACTED EMAIL]");

        // 2. Redact URLs
        sanitized = URL_PATTERN.matcher(sanitized).replaceAll("[REDACTED LINK]");

        // 3. Redact International Phone Numbers
        sanitized = INT_PHONE_PATTERN.matcher(sanitized).replaceAll("[REDACTED PHONE]");

        // 4. Redact Local Phone Numbers (starts with 0, 10-11 digits)
        sanitized = LOCAL_PHONE_PATTERN.matcher(sanitized).replaceAll("[REDACTED PHONE]");

        // 5. Redact Platform-Bypass keywords case-insensitively
        for (String keyword : BYPASS_KEYWORDS) {
            String regex = "(?i)\\b" + Pattern.quote(keyword) + "\\b";
            sanitized = Pattern.compile(regex).matcher(sanitized).replaceAll("[REDACTED]");
        }

        return sanitized;
    }
}
