package com.itssum.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정. secret 은 환경변수 JWT_SECRET 으로만 받고, HS256 키 길이(32바이트)보다 짧으면 기동을 막는다.
 */
@ConfigurationProperties("jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("jwt.secret 은 32바이트 이상이어야 합니다.");
        }
        Objects.requireNonNull(accessTokenTtl, "jwt.access-token-ttl");
        Objects.requireNonNull(refreshTokenTtl, "jwt.refresh-token-ttl");
    }
}
