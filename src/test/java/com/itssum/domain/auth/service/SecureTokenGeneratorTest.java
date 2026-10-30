package com.itssum.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SecureTokenGeneratorTest {

    private final SecureTokenGenerator generator = new SecureTokenGenerator();

    @Test
    void generatesUrlSafe43CharTokensThatDiffer() {
        String first = generator.generate();
        String second = generator.generate();

        assertTrue(first.matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(first, second);
    }

    @Test
    void hashIsLowercaseSha256Hex() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", generator.hash("abc"));
    }

    @Test
    void verificationCodeIsSixDigits() {
        for (int i = 0; i < 100; i++) {
            assertTrue(generator.generateVerificationCode().matches("\\d{6}"));
        }
    }
}
