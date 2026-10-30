package com.itssum.domain.auth.service;

import com.itssum.domain.auth.config.AuthProperties;
import com.itssum.domain.auth.dto.request.LoginRequest;
import com.itssum.domain.auth.dto.request.LogoutRequest;
import com.itssum.domain.auth.dto.request.RefreshTokenRequest;
import com.itssum.domain.auth.dto.request.SignUpDonorRequest;
import com.itssum.domain.auth.dto.response.AuthTokenResponse;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.repository.RegionRepository;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.TermsAgreement;
import com.itssum.domain.user.entity.TermsType;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.repository.DonorRepository;
import com.itssum.domain.user.repository.TermsAgreementRepository;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.security.AccessTokenProvider;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** 기부자 가입 필수 약관. 이 둘만 저장하고 요청의 다른 약관 값은 무시한다. */
    private static final Set<TermsType> DONOR_REQUIRED_TERMS = EnumSet.of(TermsType.SERVICE, TermsType.PRIVACY);

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final RegionRepository regionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationStore emailVerificationStore;
    private final AuthProperties authProperties;

    /** 이메일 인증 토큰은 커밋이 끝난 뒤에 소모한다. 중간에 실패하면 다시 인증하지 않고 재시도할 수 있다. */
    @Transactional
    public AuthTokenResponse signUpDonor(SignUpDonorRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        String verifiedEmail = emailVerificationStore.peekVerifiedEmail(request.emailVerificationToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED));
        if (!verifiedEmail.equals(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }
        if (!request.agreedTerms().containsAll(DONOR_REQUIRED_TERMS)) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED_TERMS_NOT_AGREED);
        }
        Region region = request.regionId() == null ? null : regionRepository.findById(request.regionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }

        User user = saveUser(email, request.password(), UserRole.DONOR);
        donorRepository.save(Donor.builder()
                .user(user)
                .region(region)
                .name(request.name())
                .birthDate(request.birthDate())
                .phone(formatPhone(request.phone()))
                .build());
        LocalDateTime agreedAt = LocalDateTime.now();
        termsAgreementRepository.saveAll(DONOR_REQUIRED_TERMS.stream()
                .map(type -> TermsAgreement.builder()
                        .user(user)
                        .termsType(type)
                        .termsVersion(authProperties.termsVersion())
                        .agreedAt(agreedAt)
                        .build())
                .toList());

        AuthTokenResponse response = issueTokens(user, false);
        consumeVerificationTokenAfterCommit(request.emailVerificationToken());
        return response;
    }

    /** 가입하지 않은 이메일, 틀린 비밀번호, 탈퇴 회원 모두 같은 에러로 응답한다. */
    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(EmailNormalizer.normalize(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        return issueTokens(user, true);
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        return issueTokens(refreshTokenService.rotate(request.refreshToken()), false);
    }

    /** 이 기기의 리프레시 토큰만 지운다. 액세스 토큰은 만료될 때까지 유효하다. */
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private User saveUser(String email, String rawPassword, UserRole role) {
        try {
            return userRepository.saveAndFlush(User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 이메일로 가입해 UQ_users_email 에 걸린 경우
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }
    }

    private AuthTokenResponse issueTokens(User user, boolean cleanUpExpired) {
        String accessToken = accessTokenProvider.issue(user.getId(), user.getRole());
        String refreshToken = refreshTokenService.issue(user, cleanUpExpired);
        return AuthTokenResponse.of(user, accessToken, refreshToken);
    }

    private void consumeVerificationTokenAfterCommit(String token) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailVerificationStore.consumeToken(token);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailVerificationStore.consumeToken(token);
            }
        });
    }

    /** 01012345678, 010-1234-5678 → 010-1234-5678 / 10자리는 011-123-4567 */
    private static String formatPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        int lastPartStart = digits.length() - 4;
        return digits.substring(0, 3) + "-" + digits.substring(3, lastPartStart) + "-" + digits.substring(lastPartStart);
    }
}
