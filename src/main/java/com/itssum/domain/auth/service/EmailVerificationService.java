package com.itssum.domain.auth.service;

import com.itssum.domain.auth.config.AuthProperties;
import com.itssum.domain.auth.dto.request.ConfirmEmailVerificationRequest;
import com.itssum.domain.auth.dto.request.SendEmailVerificationRequest;
import com.itssum.domain.auth.dto.response.EmailVerificationConfirmResponse;
import com.itssum.domain.auth.dto.response.EmailVerificationSendResponse;
import com.itssum.domain.auth.service.EmailVerificationStore.CodeCheckResult;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.mail.EmailSender;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String MAIL_SUBJECT = "[IT:SSUM] 이메일 인증번호";
    private static final String MAIL_TEXT = "인증번호 %s 을 %d분 안에 입력해 주세요.";

    private final EmailVerificationStore store;
    private final EmailSender emailSender;
    private final UserRepository userRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final AuthProperties authProperties;

    /** 발송에 실패하면 아무것도 저장하지 않으므로 재발송 간격 없이 바로 다시 요청할 수 있다. */
    public EmailVerificationSendResponse send(SendEmailVerificationRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }
        if (!store.canSend(email)) {
            throw new BusinessException(ErrorCode.AUTH_VERIFICATION_TOO_MANY_REQUESTS);
        }
        Duration codeTtl = authProperties.emailVerification().codeTtl();
        String code = tokenGenerator.generateVerificationCode();
        try {
            emailSender.send(email, MAIL_SUBJECT, MAIL_TEXT.formatted(code, codeTtl.toMinutes()));
        } catch (MailException e) {
            log.warn("인증 메일 발송 실패", e);
            throw new BusinessException(ErrorCode.AUTH_MAIL_SEND_FAILED);
        }
        store.saveCode(email, code);
        return new EmailVerificationSendResponse(codeTtl.toSeconds());
    }

    public EmailVerificationConfirmResponse confirm(ConfirmEmailVerificationRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        CodeCheckResult result = store.verifyCode(email, request.code());
        if (result == CodeCheckResult.MISMATCHED) {
            throw new BusinessException(ErrorCode.AUTH_VERIFICATION_CODE_MISMATCH);
        }
        if (result == CodeCheckResult.EXPIRED) {
            throw new BusinessException(ErrorCode.AUTH_VERIFICATION_CODE_EXPIRED);
        }
        String token = tokenGenerator.generate();
        store.saveVerifiedToken(token, email);
        return new EmailVerificationConfirmResponse(token);
    }
}
