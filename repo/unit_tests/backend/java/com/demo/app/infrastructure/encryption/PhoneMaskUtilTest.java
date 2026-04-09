package com.demo.app.infrastructure.encryption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PhoneMaskUtil - Phone number masking")
class PhoneMaskUtilTest {

    @Test
    @DisplayName("Full phone number is masked with last four digits visible")
    void testMask_fullPhoneNumber() {
        String result = PhoneMaskUtil.mask("+15551234567");

        assertEquals("***-***-4567", result);
    }

    @Test
    @DisplayName("Short input (fewer than 4 characters) returns ****")
    void testMask_shortInput() {
        assertEquals("****", PhoneMaskUtil.mask("12"));
    }

    @Test
    @DisplayName("Null input returns ****")
    void testMask_nullInput() {
        assertEquals("****", PhoneMaskUtil.mask(null));
    }

    @Test
    @DisplayName("Exactly four characters returns masked format with those four digits")
    void testMask_exactlyFourChars() {
        assertEquals("***-***-1234", PhoneMaskUtil.mask("1234"));
    }

    @Test
    @DisplayName("Five characters returns masked format with last four digits")
    void testMask_fiveChars() {
        assertEquals("***-***-2345", PhoneMaskUtil.mask("12345"));
    }

    @Test
    @DisplayName("Empty string returns ****")
    void testMask_emptyString() {
        assertEquals("****", PhoneMaskUtil.mask(""));
    }

    @Test
    @DisplayName("Three character input returns ****")
    void testMask_threeChars() {
        assertEquals("****", PhoneMaskUtil.mask("123"));
    }
}
