package com.demo.app.infrastructure.encryption;

public final class PhoneMaskUtil {

    private PhoneMaskUtil() {
    }

    public static String mask(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        String lastFour = phone.substring(phone.length() - 4);
        return "***-***-" + lastFour;
    }
}
