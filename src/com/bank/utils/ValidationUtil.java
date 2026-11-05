package com.bank.utils;

public final class ValidationUtil {
    private ValidationUtil() {
    }

    public static boolean isValidUsername(String username) {
        return username != null && username.trim().length() >= 4;
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidAmount(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }
}
