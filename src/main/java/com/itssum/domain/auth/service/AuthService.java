package com.itssum.domain.auth.service;

import com.itssum.domain.auth.config.AuthProperties;
import com.itssum.domain.auth.dto.request.LoginRequest;
import com.itssum.domain.auth.dto.request.LogoutRequest;
import com.itssum.domain.auth.dto.request.RefreshTokenRequest;
import com.itssum.domain.auth.dto.request.SignUpDonorRequest;
import com.itssum.domain.auth.dto.request.SignUpOrganizationRequest;
import com.itssum.domain.auth.dto.response.AuthTokenResponse;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.repository.RegionRepository;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.OrganizationVerification;
import com.itssum.domain.user.entity.TermsAgreement;
import com.itssum.domain.user.entity.TermsType;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.repository.DonorRepository;
import com.itssum.domain.user.repository.OrganizationRepository;
import com.itssum.domain.user.repository.OrganizationVerificationRepository;
import com.itssum.domain.user.repository.TermsAgreementRepository;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.domain.user.service.AddressGeocoder;
import com.itssum.global.common.PhoneNumbers;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.kakao.GeoPoint;
import com.itssum.global.security.AccessTokenProvider;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** 기부자 가입 필수 약관. 이 둘만 저장하고 요청의 다른 약관 값은 무시한다. */
    private static final Set<TermsType> DONOR_REQUIRED_TERMS = EnumSet.of(TermsType.SERVICE, TermsType.PRIVACY);
    /** 기관 가입 필수 약관. 가입 자격 확인(영리 사업자 제외) 동의까지 받는다. */
    private static final Set<TermsType> ORGANIZATION_REQUIRED_TERMS =
            EnumSet.of(TermsType.SERVICE, TermsType.PRIVACY, TermsType.ORG_ELIGIBILITY);

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationVerificationRepository organizationVerificationRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final RegionRepository regionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationStore emailVerificationStore;
    private final BusinessVerificationStore businessVerificationStore;
    private final AddressGeocoder addressGeocoder;
    private final TransactionTemplate transactionTemplate;
    private final AuthProperties authProperties;

    /**
     * 이메일 인증 토큰은 커밋이 끝난 뒤에 소모한다. 중간에 실패하면 다시 인증하지 않고 재시도할 수 있다.
     * 활동 지역은 파일럿 지역(경기 부천시)으로 고정한다. 고르지 않으면 부천시로 저장하고, 다른 지역은 받지 않는다.
     */
    @Transactional
    public AuthTokenResponse signUpDonor(SignUpDonorRequest request) {
        String email = requireVerifiedEmail(request.emailVerificationToken(), request.email());
        requireTerms(request.agreedTerms(), DONOR_REQUIRED_TERMS);
        long regionId = request.regionId() == null ? Region.PILOT_REGION_ID : request.regionId();
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
        if (regionId != Region.PILOT_REGION_ID) {
            throw new BusinessException(ErrorCode.REGION_NOT_SUPPORTED);
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }

        User user = saveUser(email, request.password(), UserRole.DONOR);
        donorRepository.save(Donor.builder()
                .user(user)
                .region(region)
                .name(request.name())
                .birthDate(request.birthDate())
                .phone(PhoneNumbers.format(request.phone()))
                .build());
        saveTerms(user, DONOR_REQUIRED_TERMS);

        AuthTokenResponse response = issueTokens(user, false);
        afterCommit(() -> emailVerificationStore.consumeToken(request.emailVerificationToken()));
        return response;
    }

    /**
     * 이메일 인증 토큰과 사업자 확인 토큰이 둘 다 필요하다. 사업자 정보는 확인받은 값과 같아야 하고,
     * 확인 이력을 기관에 연결한다. 두 토큰 모두 커밋이 끝난 뒤에 소모한다.
     * 주소 → 좌표 변환(카카오)은 검증을 모두 통과한 뒤 DB 트랜잭션 밖에서 하고, 저장만 트랜잭션으로 묶는다.
     */
    public AuthTokenResponse signUpOrganization(SignUpOrganizationRequest request) {
        String email = requireVerifiedEmail(request.emailVerificationToken(), request.email());
        String businessNumber = BusinessNumberNormalizer.normalize(request.businessNumber());
        String representativeName = request.representativeName().strip();
        VerifiedBusiness business = businessVerificationStore.peek(request.businessVerificationToken())
                .filter(verified -> verified.matches(businessNumber, request.openingDate(), representativeName))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_BUSINESS_NOT_VERIFIED));
        requireTerms(request.agreedTerms(), ORGANIZATION_REQUIRED_TERMS);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }
        if (organizationRepository.existsByBusinessNumber(businessNumber)) {
            throw new BusinessException(ErrorCode.AUTH_BUSINESS_NUMBER_DUPLICATED);
        }
        String address = request.address().strip();
        // 좌표가 없으면 거리순 추천에서만 빠지므로, 변환에 실패해도 가입은 그대로 진행한다
        GeoPoint location = addressGeocoder.locate(address).orElse(null);

        return transactionTemplate.execute(status -> {
            OrganizationVerification verification = organizationVerificationRepository
                    .findById(business.verificationId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_BUSINESS_NOT_VERIFIED));
            User user = saveUser(email, request.password(), UserRole.ORGANIZATION);
            Organization organization = saveOrganization(Organization.builder()
                    .user(user)
                    .name(request.name().strip())
                    .representativeName(representativeName)
                    .businessNumber(businessNumber)
                    .openingDate(request.openingDate())
                    .phone(PhoneNumbers.format(request.phone()))
                    .address(address)
                    .addressDetail(blankToNull(request.addressDetail()))
                    .latitude(location == null ? null : location.latitude())
                    .longitude(location == null ? null : location.longitude())
                    .verifiedAt(verification.getCheckedAt())
                    .build());
            verification.linkOrganization(organization);
            saveTerms(user, ORGANIZATION_REQUIRED_TERMS);

            AuthTokenResponse response = issueTokens(user, false);
            afterCommit(() -> {
                emailVerificationStore.consumeToken(request.emailVerificationToken());
                businessVerificationStore.consume(request.businessVerificationToken());
            });
            return response;
        });
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

    /** 인증 토큰에 묶인 이메일과 요청 이메일이 같아야 한다. 정규화한 이메일을 돌려준다. */
    private String requireVerifiedEmail(String emailVerificationToken, String requestEmail) {
        String email = EmailNormalizer.normalize(requestEmail);
        String verifiedEmail = emailVerificationStore.peekVerifiedEmail(emailVerificationToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED));
        if (!verifiedEmail.equals(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }
        return email;
    }

    private static void requireTerms(List<TermsType> agreedTerms, Set<TermsType> requiredTerms) {
        if (!agreedTerms.containsAll(requiredTerms)) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED_TERMS_NOT_AGREED);
        }
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

    private Organization saveOrganization(Organization organization) {
        try {
            return organizationRepository.saveAndFlush(organization);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 사업자번호로 가입해 UQ_organizations_business_number 에 걸린 경우
            throw new BusinessException(ErrorCode.AUTH_BUSINESS_NUMBER_DUPLICATED);
        }
    }

    /** 필수 약관만 저장하고 요청의 다른 약관 값은 무시한다. */
    private void saveTerms(User user, Set<TermsType> requiredTerms) {
        LocalDateTime agreedAt = LocalDateTime.now();
        termsAgreementRepository.saveAll(requiredTerms.stream()
                .map(type -> TermsAgreement.builder()
                        .user(user)
                        .termsType(type)
                        .termsVersion(authProperties.termsVersion())
                        .agreedAt(agreedAt)
                        .build())
                .toList());
    }

    private AuthTokenResponse issueTokens(User user, boolean cleanUpExpired) {
        String accessToken = accessTokenProvider.issue(user.getId(), user.getRole());
        String refreshToken = refreshTokenService.issue(user, cleanUpExpired);
        return AuthTokenResponse.of(user, accessToken, refreshToken);
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
