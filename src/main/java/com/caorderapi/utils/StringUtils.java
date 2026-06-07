package com.caorderapi.utils;

public class StringUtils {
    public static String normalizeKey(String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
