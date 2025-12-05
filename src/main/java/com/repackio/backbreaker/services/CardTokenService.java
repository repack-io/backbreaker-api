package com.repackio.backbreaker.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Service for generating and validating tamper-proof card tokens using HMAC-SHA256.
 *
 * Token format: {cardId}.{timestamp}.{signature}
 * - cardId: The series card ID
 * - timestamp: Unix timestamp (optional expiry check)
 * - signature: HMAC-SHA256(cardId.timestamp, secretKey)
 *
 * The entire token is then Base64 URL-encoded for use in URLs.
 */
@Slf4j
@Service
public class CardTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DELIMITER = ".";

    private final String secretKey;

    public CardTokenService(@Value("${card.token.secret:default-secret-change-me-in-production}") String secretKey) {
        this.secretKey = secretKey;
        if ("default-secret-change-me-in-production".equals(secretKey)) {
            log.warn("Using default card token secret! Please set 'card.token.secret' in application properties.");
        }
    }

    /**
     * Generate a signed token for a card ID.
     *
     * @param cardId The series card ID
     * @return Base64 URL-encoded signed token
     */
    public String generateToken(Long cardId) {
        long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
        String payload = cardId + DELIMITER + timestamp;
        String signature = generateSignature(payload);
        String fullToken = payload + DELIMITER + signature;

        // Base64 URL-encode the entire token
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(fullToken.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validate and extract the card ID from a signed token.
     *
     * @param token Base64 URL-encoded signed token
     * @return The card ID if valid
     * @throws IllegalArgumentException if token is invalid or tampered
     */
    public Long validateAndExtract(String token) {
        try {
            // Decode the Base64 URL-encoded token
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);

            // Split into parts: cardId.timestamp.signature
            String[] parts = decoded.split("\\" + DELIMITER);
//            if (parts.length != 3) {
//                throw new IllegalArgumentException("Invalid token format");
//            }

            String cardId = parts[0];
            String timestamp = parts[1];
            String providedSignature = parts[2];

            // Verify signature
            String payload = cardId + DELIMITER + timestamp;
            String expectedSignature = generateSignature(payload);

            if (!expectedSignature.equals(providedSignature)) {
                log.warn("Token signature mismatch. Token may have been tampered with.");
                throw new IllegalArgumentException("Invalid token signature");
            }

            // Optional: Check timestamp for expiry (currently not enforced)
            // You can add expiry logic here if needed

            return Long.parseLong(cardId);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating token", e);
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    /**
     * Generate HMAC-SHA256 signature for the given payload.
     */
    private String generateSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);

            byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string for readability in logs
            return bytesToHex(signatureBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating signature", e);
            throw new RuntimeException("Failed to generate token signature", e);
        }
    }

    /**
     * Convert bytes to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
