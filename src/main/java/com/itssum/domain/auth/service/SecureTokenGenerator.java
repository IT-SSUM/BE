package com.itssum.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 리프레시 토큰·이메일 인증 토큰 원문(256비트 Base64URL 43자)과 그 SHA-256 해시, 6자리 인증번호를 만든다.
 */
@Component
public class SecureTokenGenerator {

    private static final int TOKEN_BYTES = 32;
    private static final int VERIFICATION_CODE_BOUND = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** DB 에는 원문 대신 이 값(소문자 hex 64자)을 저장한다. */
    public String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", e);
        }
    }

    public String generateVerificationCode() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(VERIFICATION_CODE_BOUND));
    }
}
