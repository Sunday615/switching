package com.example.switching.common.util;

public final class MaskingUtil {

    private MaskingUtil() {}

    /**
     * Masks an account number, showing only the last 4 digits.
     * e.g. "1234567890" → "******7890"
     */
    public static String maskAccount(String accountNo) {
        if (accountNo == null) return null;
        if (accountNo.length() <= 4) return "****";
        return "*".repeat(accountNo.length() - 4) + accountNo.substring(accountNo.length() - 4);
    }

    /**
     * Masks a generic sensitive value, showing only the last N visible chars.
     */
    public static String maskSensitive(String value, int visibleSuffix) {
        if (value == null) return null;
        if (value.length() <= visibleSuffix) return "*".repeat(value.length());
        return "*".repeat(value.length() - visibleSuffix) + value.substring(value.length() - visibleSuffix);
    }
}
