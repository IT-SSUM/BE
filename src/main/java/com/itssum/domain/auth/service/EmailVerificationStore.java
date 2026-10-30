package com.itssum.domain.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.itssum.domain.auth.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 이메일 인증번호와 인증 완료 토큰을 메모리(Caffeine)에 둔다.
 * 서버 한 대 기준이다. 재시작하면 진행 중인 인증은 사라지고, 서버가 여러 대가 되면 공유 저장소(Redis 등)로 옮긴다.
 */
@Component
public class EmailVerificationStore {

    private static final int MAX_ENTRIES = 10_000;

    private final AuthProperties.EmailVerification policy;
    private final Ticker ticker;
    private final Cache<String, PendingCode> codes;
    private final Cache<String, String> verifiedTokens;

    @Autowired
    public EmailVerificationStore(AuthProperties properties) {
        this(properties, Ticker.systemTicker());
    }

    EmailVerificationStore(AuthProperties properties, Ticker ticker) {
        this.policy = properties.emailVerification();
        this.ticker = ticker;
        // 오답 횟수를 고치면 쓰기 시각이 갱신되므로 인증번호 만료는 sentAt 으로 직접 판단하고, 캐시 만료는 메모리 정리용이다
        this.codes = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfterWrite(policy.codeTtl())
                .maximumSize(MAX_ENTRIES)
                .build();
        this.verifiedTokens = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfterWrite(policy.tokenTtl())
                .maximumSize(MAX_ENTRIES)
                .build();
    }

    /** 마지막 발송 후 재발송 간격이 지났는지 */
    public boolean canSend(String email) {
        PendingCode pending = codes.getIfPresent(email);
        return pending == null || elapsedSince(pending.sentAtNanos()).compareTo(policy.resendCooldown()) >= 0;
    }

    /** 새 인증번호를 저장한다. 이전 번호와 오답 횟수는 버린다. */
    public void saveCode(String email, String code) {
        codes.put(email, new PendingCode(code, ticker.read(), 0));
    }

    /** 맞으면 번호를 지운다. 최대 오답 횟수에 닿으면 번호를 버리고 EXPIRED 를 돌려준다. */
    public CodeCheckResult verifyCode(String email, String code) {
        AtomicReference<CodeCheckResult> result = new AtomicReference<>(CodeCheckResult.EXPIRED);
        codes.asMap().computeIfPresent(email, (key, pending) -> {
            if (elapsedSince(pending.sentAtNanos()).compareTo(policy.codeTtl()) >= 0) {
                return null;
            }
            if (MessageDigest.isEqual(bytes(pending.code()), bytes(code))) {
                result.set(CodeCheckResult.MATCHED);
                return null;
            }
            int attempts = pending.attempts() + 1;
            if (attempts >= policy.maxAttempts()) {
                return null;
            }
            result.set(CodeCheckResult.MISMATCHED);
            return new PendingCode(pending.code(), pending.sentAtNanos(), attempts);
        });
        return result.get();
    }

    public void saveVerifiedToken(String token, String email) {
        verifiedTokens.put(token, email);
    }

    /** 토큰에 묶인 이메일. 소모하지 않는다 (가입 커밋 뒤 consumeToken). */
    public Optional<String> peekVerifiedEmail(String token) {
        return Optional.ofNullable(verifiedTokens.getIfPresent(token));
    }

    public void consumeToken(String token) {
        verifiedTokens.invalidate(token);
    }

    private Duration elapsedSince(long nanos) {
        return Duration.ofNanos(ticker.read() - nanos);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public enum CodeCheckResult {
        MATCHED,
        MISMATCHED,
        EXPIRED
    }

    private record PendingCode(String code, long sentAtNanos, int attempts) {
    }
}
