package com.itssum.domain.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.itssum.domain.auth.config.AuthProperties;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 사업자 확인 토큰을 메모리(Caffeine)에 둔다. EmailVerificationStore 와 같이 서버 한 대 기준이다.
 */
@Component
public class BusinessVerificationStore {

    private static final int MAX_ENTRIES = 10_000;

    private final Cache<String, VerifiedBusiness> tokens;

    @Autowired
    public BusinessVerificationStore(AuthProperties properties) {
        this(properties, Ticker.systemTicker());
    }

    BusinessVerificationStore(AuthProperties properties, Ticker ticker) {
        this.tokens = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfterWrite(properties.businessVerification().tokenTtl())
                .maximumSize(MAX_ENTRIES)
                .build();
    }

    public void save(String token, VerifiedBusiness business) {
        tokens.put(token, business);
    }

    /** 소모하지 않는다 (가입 커밋 뒤 consume). */
    public Optional<VerifiedBusiness> peek(String token) {
        return Optional.ofNullable(tokens.getIfPresent(token));
    }

    public void consume(String token) {
        tokens.invalidate(token);
    }
}
