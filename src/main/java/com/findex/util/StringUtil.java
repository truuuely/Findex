package com.findex.util;

public class StringUtil {

    public static String requireNonBlank(String string) {
        if (string == null || string.isBlank()) {
            throw new IllegalArgumentException(string + " must not be blank");
        }
        return string;
    }
}
