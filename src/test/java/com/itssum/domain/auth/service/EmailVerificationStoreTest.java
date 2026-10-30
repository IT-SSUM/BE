package com.itssum.domain.auth.service;

import static com.itssum.domain.auth.service.EmailVerificationStore.CodeCheckResult.EXPIRED;
import static com.itssum.domain.auth.service.EmailVerificationStore.CodeCheckResult.MATCHED;
import static com.itssum.domain.auth.service.EmailVerificationStore.CodeCheckResult.MISMATCHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itssum.domain.auth.config.AuthProperties;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmailVerificationStoreTest {

    private static final String EMAIL = "donor@example.com";
    private static final AuthProperties PROPERTIES = new AuthProperties("v1", new AuthProperties.EmailVerification(
            Duration.ofMinutes(5), Duration.ofSeconds(60), 5, Duration.ofMinutes(30)));

    private final FakeTicker ticker = new FakeTicker();
    private final EmailVerificationStore store = new EmailVerificationStore(PROPERTIES, ticker);

    @Test
    void resendIsBlockedDuringCooldown() {
        assertTrue(store.canSend(EMAIL));
        store.saveCode(EMAIL, "123456");

        ticker.advance(Duration.ofSeconds(59));
        assertFalse(store.canSend(EMAIL));

        ticker.advance(Duration.ofSeconds(1));
        assertTrue(store.canSend(EMAIL));
    }

    @Test
    void correctCodeMatchesOnlyOnce() {
        store.saveCode(EMAIL, "123456");

        assertEquals(MATCHED, store.verifyCode(EMAIL, "123456"));
        assertEquals(EXPIRED, store.verifyCode(EMAIL, "123456"));
    }

    @Test
    void fifthWrongAttemptDiscardsCode() {
        store.saveCode(EMAIL, "123456");

        for (int i = 0; i < 4; i++) {
            assertEquals(MISMATCHED, store.verifyCode(EMAIL, "000000"));
        }
        assertEquals(EXPIRED, store.verifyCode(EMAIL, "000000"));
        assertEquals(EXPIRED, store.verifyCode(EMAIL, "123456"));
        // 번호를 버려도 재발송 간격은 남는다
        assertFalse(store.canSend(EMAIL));
    }

    @Test
    void codeExpiresFiveMinutesAfterSendingEvenIfAttemptsWereUpdated() {
        store.saveCode(EMAIL, "123456");
        ticker.advance(Duration.ofMinutes(4));
        // 오답 기록으로 캐시 쓰기 시각이 갱신돼도 만료는 발송 시각 기준이다
        assertEquals(MISMATCHED, store.verifyCode(EMAIL, "000000"));

        ticker.advance(Duration.ofMinutes(1));
        assertEquals(EXPIRED, store.verifyCode(EMAIL, "123456"));
    }

    @Test
    void resendReplacesCodeAndResetsAttempts() {
        store.saveCode(EMAIL, "111111");
        for (int i = 0; i < 4; i++) {
            store.verifyCode(EMAIL, "000000");
        }

        store.saveCode(EMAIL, "222222");

        assertEquals(MISMATCHED, store.verifyCode(EMAIL, "111111"));
        assertEquals(MATCHED, store.verifyCode(EMAIL, "222222"));
    }

    @Test
    void verifiedTokenCanBePeekedUntilConsumed() {
        store.saveVerifiedToken("token-a", EMAIL);

        assertEquals(Optional.of(EMAIL), store.peekVerifiedEmail("token-a"));
        assertEquals(Optional.of(EMAIL), store.peekVerifiedEmail("token-a"));

        store.consumeToken("token-a");
        assertEquals(Optional.empty(), store.peekVerifiedEmail("token-a"));
    }

    @Test
    void verifiedTokenExpiresAfterThirtyMinutes() {
        store.saveVerifiedToken("token-b", EMAIL);

        ticker.advance(Duration.ofMinutes(30).plusSeconds(1));

        assertEquals(Optional.empty(), store.peekVerifiedEmail("token-b"));
    }
}
