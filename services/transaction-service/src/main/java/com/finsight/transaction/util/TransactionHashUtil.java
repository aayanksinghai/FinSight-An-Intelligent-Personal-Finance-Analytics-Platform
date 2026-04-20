package com.finsight.transaction.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.math.BigDecimal;

public class TransactionHashUtil {
    
    public static String generateHash(String ownerEmail, Instant occurredAt, BigDecimal amount, String type, String rawDescription) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Standardize format: handle nulls gracefully
            String email = ownerEmail != null ? ownerEmail.trim().toLowerCase() : "";
            long time = occurredAt != null ? occurredAt.toEpochMilli() : 0;
            String amt = amount != null ? amount.stripTrailingZeros().toPlainString() : "0";
            String t = type != null ? type.toUpperCase() : "";
            String desc = rawDescription != null ? rawDescription.trim().toLowerCase() : "";

            String data = String.format("%s|%d|%s|%s|%s", email, time, amt, t, desc);
            byte[] encodedhash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
