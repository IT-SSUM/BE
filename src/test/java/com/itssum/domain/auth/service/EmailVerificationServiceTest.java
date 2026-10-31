package com.itssum.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.domain.auth.config.AuthProperties;
import com.itssum.domain.auth.dto.request.ConfirmEmailVerificationRequest;
import com.itssum.domain.auth.dto.request.SendEmailVerificationRequest;
import com.itssum.domain.auth.service.EmailVerificationStore.CodeCheckResult;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.mail.EmailSender;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;

class EmailVerificationServiceTest {

    private static final String EMAIL = "donor@example.com";
    private static final AuthProperties PROPERTIES = new AuthProperties("v1", new AuthProperties.EmailVerification(
            Duration.ofMinutes(5), Duration.ofSeconds(60), 5, Duration.ofMinutes(30)),
            new AuthProperties.BusinessVerification(Duration.ofMinutes(30)));

    private final EmailVerificationStore store = new EmailVerificationStore(PROPERTIES, new FakeTicker());
    private final EmailSender emailSender = mock(EmailSender.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SecureTokenGenerator tokenGenerator = mock(SecureTokenGenerator.class);
    private final EmailVerificationService service =
            new EmailVerificationService(store, emailSender, userRepository, tokenGenerator, PROPERTIES);

    @BeforeEach
    void setUp() {
        when(tokenGenerator.generateVerificationCode()).thenReturn("123456");
        when(tokenGenerator.generate()).thenReturn("verify-token");
    }

    @Test
    void sendMailsCodeToNormalizedEmailAndStoresIt() {
        assertEquals(300, service.send(new SendEmailVerificationRequest(" Donor@Example.com ")).expiresInSeconds());

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(EMAIL), eq("[IT:SSUM] 이메일 인증번호"), text.capture());
        assertEquals("인증번호 123456 을 5분 안에 입력해 주세요.", text.getValue());
        assertEquals(CodeCheckResult.MATCHED, store.verifyCode(EMAIL, "123456"));
    }

    @Test
    void sendRejectsRegisteredEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertError(ErrorCode.AUTH_EMAIL_DUPLICATED, () -> service.send(new SendEmailVerificationRequest(EMAIL)));
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void sendWithinCooldownIsRejected() {
        service.send(new SendEmailVerificationRequest(EMAIL));

        assertError(ErrorCode.AUTH_VERIFICATION_TOO_MANY_REQUESTS,
                () -> service.send(new SendEmailVerificationRequest(EMAIL)));
    }

    @Test
    void mailFailureStoresNothing() {
        doThrow(new MailSendException("smtp down")).when(emailSender).send(anyString(), anyString(), anyString());

        assertError(ErrorCode.AUTH_MAIL_SEND_FAILED, () -> service.send(new SendEmailVerificationRequest(EMAIL)));
        assertTrue(store.canSend(EMAIL));
        assertEquals(CodeCheckResult.EXPIRED, store.verifyCode(EMAIL, "123456"));
    }

    @Test
    void confirmIssuesTokenBoundToEmail() {
        store.saveCode(EMAIL, "123456");

        assertEquals("verify-token",
                service.confirm(new ConfirmEmailVerificationRequest(" Donor@Example.com ", "123456"))
                        .emailVerificationToken());
        assertEquals(Optional.of(EMAIL), store.peekVerifiedEmail("verify-token"));
    }

    @Test
    void confirmWithWrongCodeIsMismatch() {
        store.saveCode(EMAIL, "123456");

        assertError(ErrorCode.AUTH_VERIFICATION_CODE_MISMATCH,
                () -> service.confirm(new ConfirmEmailVerificationRequest(EMAIL, "000000")));
    }

    @Test
    void confirmWithoutRequestIsExpired() {
        assertError(ErrorCode.AUTH_VERIFICATION_CODE_EXPIRED,
                () -> service.confirm(new ConfirmEmailVerificationRequest(EMAIL, "123456")));
    }

    private static void assertError(ErrorCode expected, Executable executable) {
        BusinessException e = assertThrows(BusinessException.class, executable);
        assertEquals(expected, e.getErrorCode());
    }
}
