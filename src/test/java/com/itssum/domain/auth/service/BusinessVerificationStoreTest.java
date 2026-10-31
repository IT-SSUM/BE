package com.itssum.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itssum.domain.auth.config.AuthProperties;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BusinessVerificationStoreTest {

    private static final AuthProperties PROPERTIES = new AuthProperties("v1",
            new AuthProperties.EmailVerification(Duration.ofMinutes(5), Duration.ofSeconds(60), 5, Duration.ofMinutes(30)),
            new AuthProperties.BusinessVerification(Duration.ofMinutes(30)));
    private static final VerifiedBusiness BUSINESS =
            new VerifiedBusiness(7L, "1234567890", LocalDate.of(2015, 3, 2), "김수현");

    private final FakeTicker ticker = new FakeTicker();
    private final BusinessVerificationStore store = new BusinessVerificationStore(PROPERTIES, ticker);

    @Test
    void tokenIsReadableUntilConsumed() {
        store.save("business-token", BUSINESS);

        assertEquals(Optional.of(BUSINESS), store.peek("business-token"));
        assertEquals(Optional.of(BUSINESS), store.peek("business-token"));

        store.consume("business-token");
        assertTrue(store.peek("business-token").isEmpty());
    }

    @Test
    void tokenExpiresAfterTtl() {
        store.save("business-token", BUSINESS);

        ticker.advance(Duration.ofMinutes(29));
        assertTrue(store.peek("business-token").isPresent());

        ticker.advance(Duration.ofMinutes(1));
        assertTrue(store.peek("business-token").isEmpty());
    }

    @Test
    void matchesComparesVerifiedValues() {
        assertTrue(BUSINESS.matches("1234567890", LocalDate.of(2015, 3, 2), "김수현"));
        assertFalse(BUSINESS.matches("1234567891", LocalDate.of(2015, 3, 2), "김수현"));
        assertFalse(BUSINESS.matches("1234567890", LocalDate.of(2015, 3, 3), "김수현"));
        assertFalse(BUSINESS.matches("1234567890", LocalDate.of(2015, 3, 2), "김수연"));
    }
}
