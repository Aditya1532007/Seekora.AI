package com.seekora.util;

import java.util.regex.Pattern;

public final class Validator {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validator() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isEmail(String value) {
        return value != null && EMAIL.matcher(value.trim()).matches();
    }

    public static boolean isStrongEnough(String password) {
        return password != null && password.length() >= 6;
    }

    public static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
