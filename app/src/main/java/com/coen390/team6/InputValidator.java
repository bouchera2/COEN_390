package com.coen390.team6;

import android.widget.EditText;
import android.widget.Toast;
import android.content.Context;

/**
 * InputValidator — Protects against XSS, script injection, and invalid inputs.
 * Use this on ALL user input fields before saving to Firestore or displaying.
 */
public class InputValidator {

    // Characters that could be used for XSS or script injection
    private static final String[] DANGEROUS_PATTERNS = {
            "<script", "</script", "javascript:", "onerror=", "onload=",
            "<iframe", "<img", "<svg", "onclick=", "onfocus=",
            "<style", "</style", "expression(", "url(", "eval(",
            "document.", "window.", "alert(", ".cookie", "innerHTML",
            "<object", "<embed", "<applet", "<form", "<input",
            "&#", "%3C", "%3E", "%22", "%27"
    };

    /**
     * Sanitize a string by removing dangerous HTML/script characters.
     * @param input raw user input
     * @return sanitized string safe for Firestore and display
     */
    public static String sanitize(String input) {
        if (input == null) return "";

        String sanitized = input.trim();

        // Remove HTML tags
        sanitized = sanitized.replaceAll("<[^>]*>", "");

        // Escape special characters
        sanitized = sanitized.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");

        // Remove any remaining script-like patterns
        for (String pattern : DANGEROUS_PATTERNS) {
            sanitized = sanitized.replace(pattern, "");
        }

        return sanitized;
    }

    /**
     * Validate that a string is not empty.
     * Sets error on the EditText if invalid.
     * @return true if valid
     */
    public static boolean validateNotEmpty(EditText editText, String fieldName) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) {
            editText.setError(fieldName + " is required");
            editText.requestFocus();
            return false;
        }
        if (containsDangerousInput(text)) {
            editText.setError("Invalid input — special characters not allowed");
            editText.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Validate email format.
     */
    public static boolean validateEmail(EditText editText) {
        String email = editText.getText().toString().trim();
        if (email.isEmpty()) {
            editText.setError("Email is required");
            editText.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editText.setError("Invalid email format");
            editText.requestFocus();
            return false;
        }
        if (containsDangerousInput(email)) {
            editText.setError("Invalid input");
            editText.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Validate a numeric field (age, weight, height, HR).
     * @param min minimum allowed value
     * @param max maximum allowed value
     */
    public static boolean validateNumber(EditText editText, String fieldName, int min, int max) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) {
            editText.setError(fieldName + " is required");
            editText.requestFocus();
            return false;
        }
        try {
            double value = Double.parseDouble(text);
            if (value < min || value > max) {
                editText.setError(fieldName + " must be between " + min + " and " + max);
                editText.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            editText.setError("Invalid number");
            editText.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Validate phone number format.
     */
    public static boolean validatePhone(EditText editText) {
        String phone = editText.getText().toString().trim();
        if (phone.isEmpty()) return true; // Phone is optional
        // Allow only digits, +, -, spaces, parentheses
        if (!phone.matches("[+\\-()\\s\\d]{7,20}")) {
            editText.setError("Invalid phone number");
            editText.requestFocus();
            return false;
        }
        if (containsDangerousInput(phone)) {
            editText.setError("Invalid input");
            editText.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Validate password (min 6 chars, no scripts).
     */
    public static boolean validatePassword(EditText editText) {
        String password = editText.getText().toString().trim();
        if (password.isEmpty()) {
            editText.setError("Password is required");
            editText.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            editText.setError("Password must be at least 6 characters");
            editText.requestFocus();
            return false;
        }
        if (containsDangerousInput(password)) {
            editText.setError("Invalid input — special characters not allowed");
            editText.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Check if input contains any dangerous patterns.
     */
    public static boolean containsDangerousInput(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        for (String pattern : DANGEROUS_PATTERNS) {
            if (lower.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get sanitized text from an EditText.
     */
    public static String getSanitizedText(EditText editText) {
        return sanitize(editText.getText().toString());
    }
}
