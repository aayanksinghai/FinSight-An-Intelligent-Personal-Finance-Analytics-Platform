package com.finsight.user.api.auth;

public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";
    public static final String MESSAGE =
            "Password must have upper, lower, digit, special character and be at least 8 chars";
    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }
}

