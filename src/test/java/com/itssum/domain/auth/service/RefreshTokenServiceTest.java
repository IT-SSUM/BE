package com.itssum.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.domain.auth.entity.RefreshToken;
import com.itssum.domain.auth.repository.RefreshTokenRepository;
import com.itssum.domain.user.entity.User;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.security.JwtProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshTokenServiceTest {

    private static final JwtProperties JWT = new JwtProperties(
            "test-jwt-secret-0123456789-abcdefghijklmnop", Duration.ofMinutes(30), Duration.ofDays(14));

    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final SecureTokenGenerator generator = new SecureTokenGenerator();
    private final RefreshTokenService service = new RefreshTokenService(repository, generator, JWT);

    @Test
    void issueStoresOnlyHashAndReturnsRawToken() {
        User user = user(1L, true);

        String raw = service.issue(user, false);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertEquals(generator.hash(raw), saved.getValue().getTokenHash());
        assertSame(user, saved.getValue().getUser());
        Duration ttl = Duration.between(LocalDateTime.now(), saved.getValue().getExpiresAt());
        assertTrue(ttl.compareTo(Duration.ofDays(14).minusMinutes(1)) > 0);
        assertTrue(ttl.compareTo(Duration.ofDays(14)) <= 0);
        verify(repository, never()).deleteExpiredByUserId(any(), any());
    }

    @Test
    void issueOnLoginDeletesExpiredTokensOfThatUserFirst() {
        service.issue(user(1L, true), true);

        verify(repository).deleteExpiredByUserId(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void rotateDeletesTokenAndReturnsUser() {
        User user = user(1L, true);
        stubToken("raw", token(5L, user, LocalDateTime.now().plusDays(1)));
        when(repository.deleteByIdAndReturnCount(5L)).thenReturn(1);

        assertSame(user, service.rotate("raw"));
        verify(repository).deleteByIdAndReturnCount(5L);
    }

    @Test
    void rotateRejectsUnknownToken() {
        when(repository.findWithUserByTokenHash(generator.hash("raw"))).thenReturn(Optional.empty());

        assertInvalidRefreshToken(() -> service.rotate("raw"));
    }

    @Test
    void rotateRejectsExpiredTokenWithoutDeleting() {
        stubToken("raw", token(5L, user(1L, true), LocalDateTime.now().minusSeconds(1)));

        assertInvalidRefreshToken(() -> service.rotate("raw"));
        verify(repository, never()).deleteByIdAndReturnCount(any());
    }

    @Test
    void rotateRejectsTokenAlreadyRotatedByAnotherRequest() {
        stubToken("raw", token(5L, user(1L, true), LocalDateTime.now().plusDays(1)));
        when(repository.deleteByIdAndReturnCount(5L)).thenReturn(0);

        assertInvalidRefreshToken(() -> service.rotate("raw"));
    }

    @Test
    void rotateRejectsInactiveUser() {
        stubToken("raw", token(5L, user(1L, false), LocalDateTime.now().plusDays(1)));
        when(repository.deleteByIdAndReturnCount(5L)).thenReturn(1);

        assertInvalidRefreshToken(() -> service.rotate("raw"));
    }

    @Test
    void revokeDeletesByHash() {
        service.revoke("raw");

        verify(repository).deleteByTokenHash(generator.hash("raw"));
    }

    private void stubToken(String raw, RefreshToken token) {
        when(repository.findWithUserByTokenHash(generator.hash(raw))).thenReturn(Optional.of(token));
    }

    private static User user(Long id, boolean active) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.isActive()).thenReturn(active);
        return user;
    }

    private static RefreshToken token(Long id, User user, LocalDateTime expiresAt) {
        RefreshToken token = RefreshToken.builder().user(user).tokenHash("h".repeat(64)).expiresAt(expiresAt).build();
        ReflectionTestUtils.setField(token, "id", id);
        return token;
    }

    private static void assertInvalidRefreshToken(Executable executable) {
        BusinessException e = assertThrows(BusinessException.class, executable);
        assertEquals(ErrorCode.AUTH_INVALID_REFRESH_TOKEN, e.getErrorCode());
    }
}
