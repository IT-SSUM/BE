package com.itssum.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.itssum.domain.user.entity.VerificationResult;
import com.itssum.domain.user.entity.VerificationType;
import com.itssum.domain.user.repository.DonorRepository;
import com.itssum.domain.user.repository.OrganizationRepository;
import com.itssum.domain.user.repository.OrganizationVerificationRepository;
import com.itssum.domain.user.repository.TermsAgreementRepository;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.kakao.GeoPoint;
import com.itssum.global.infra.kakao.KakaoApiException;
import com.itssum.global.infra.kakao.KakaoLocalClient;
import com.itssum.global.security.AccessTokenProvider;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class AuthServiceTest {

    private static final String EMAIL = "donor@example.com";
    private static final String ORG_EMAIL = "org@example.com";
    private static final LocalDate OPENING_DATE = LocalDate.of(2015, 3, 2);
    private static final VerifiedBusiness VERIFIED_BUSINESS =
            new VerifiedBusiness(7L, "1234567890", OPENING_DATE, "김수현");
    private static final List<TermsType> ORG_TERMS =
            List.of(TermsType.SERVICE, TermsType.PRIVACY, TermsType.ORG_ELIGIBILITY);
    private static final String ORG_ADDRESS = "서울 구로구 가마산로 12";
    private static final AuthProperties PROPERTIES = new AuthProperties("v1", new AuthProperties.EmailVerification(
            Duration.ofMinutes(5), Duration.ofSeconds(60), 5, Duration.ofMinutes(30)),
            new AuthProperties.BusinessVerification(Duration.ofMinutes(30)));

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DonorRepository donorRepository = mock(DonorRepository.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final OrganizationVerificationRepository organizationVerificationRepository =
            mock(OrganizationVerificationRepository.class);
    private final TermsAgreementRepository termsAgreementRepository = mock(TermsAgreementRepository.class);
    private final RegionRepository regionRepository = mock(RegionRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AccessTokenProvider accessTokenProvider = mock(AccessTokenProvider.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final EmailVerificationStore emailVerificationStore = mock(EmailVerificationStore.class);
    private final BusinessVerificationStore businessVerificationStore = mock(BusinessVerificationStore.class);
    private final KakaoLocalClient kakaoLocalClient = mock(KakaoLocalClient.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final AuthService service = new AuthService(userRepository, donorRepository, organizationRepository,
            organizationVerificationRepository, termsAgreementRepository, regionRepository, passwordEncoder,
            accessTokenProvider, refreshTokenService, emailVerificationStore, businessVerificationStore,
            kakaoLocalClient, new TransactionTemplate(transactionManager), PROPERTIES);
    private final Region bucheon = mock(Region.class);

    @BeforeEach
    void setUp() {
        // 파일럿 지역 경기 부천시 (data.sql 에서 region_id = 1 로 넣는다)
        when(regionRepository.findById(1L)).thenReturn(Optional.of(bucheon));
    }

    @Test
    void signUpDonorSavesUserDonorAndRequiredTermsThenIssuesTokens() {
        Region region = mock(Region.class);
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.of(EMAIL));
        when(regionRepository.findById(12L)).thenReturn(Optional.of(region));
        when(passwordEncoder.encode("abcd1234")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        when(accessTokenProvider.issue(1L, UserRole.DONOR)).thenReturn("access");
        when(refreshTokenService.issue(any(User.class), eq(false))).thenReturn("refresh");

        AuthTokenResponse response = service.signUpDonor(signUp(" Donor@Example.com ", "01012345678", 12L,
                List.of(TermsType.SERVICE, TermsType.PRIVACY, TermsType.ORG_ELIGIBILITY)));

        assertEquals(new AuthTokenResponse(1L, UserRole.DONOR, "access", "refresh"), response);
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(user.capture());
        assertEquals(EMAIL, user.getValue().getEmail());
        assertEquals("hashed", user.getValue().getPassword());
        assertEquals(UserRole.DONOR, user.getValue().getRole());
        ArgumentCaptor<Donor> donor = ArgumentCaptor.forClass(Donor.class);
        verify(donorRepository).save(donor.capture());
        assertSame(user.getValue(), donor.getValue().getUser());
        assertSame(region, donor.getValue().getRegion());
        assertEquals("최윤서", donor.getValue().getName());
        assertEquals("010-1234-5678", donor.getValue().getPhone());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TermsAgreement>> terms = ArgumentCaptor.forClass(List.class);
        verify(termsAgreementRepository).saveAll(terms.capture());
        assertEquals(List.of(TermsType.SERVICE, TermsType.PRIVACY),
                terms.getValue().stream().map(TermsAgreement::getTermsType).toList());
        assertEquals(List.of("v1", "v1"), terms.getValue().stream().map(TermsAgreement::getTermsVersion).toList());
        verify(emailVerificationStore).consumeToken("verify-token");
    }

    @Test
    void signUpWithoutRegionUsesBucheonAndTenDigitPhone() {
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.of(EMAIL));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.signUpDonor(signUp(EMAIL, "0111234567", null, List.of(TermsType.SERVICE, TermsType.PRIVACY)));

        ArgumentCaptor<Donor> donor = ArgumentCaptor.forClass(Donor.class);
        verify(donorRepository).save(donor.capture());
        assertSame(bucheon, donor.getValue().getRegion());
        assertEquals("011-123-4567", donor.getValue().getPhone());
    }

    @Test
    void signUpRequiresVerificationTokenForSameEmail() {
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.empty());
        assertError(ErrorCode.AUTH_EMAIL_NOT_VERIFIED, () -> service.signUpDonor(validSignUp()));

        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.of("other@example.com"));
        assertError(ErrorCode.AUTH_EMAIL_NOT_VERIFIED, () -> service.signUpDonor(validSignUp()));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void signUpRequiresServiceAndPrivacyTerms() {
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.of(EMAIL));

        assertError(ErrorCode.AUTH_REQUIRED_TERMS_NOT_AGREED,
                () -> service.signUpDonor(signUp(EMAIL, "01012345678", null, List.of(TermsType.SERVICE))));
    }

    @Test
    void signUpRejectsUnknownRegion() {
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.of(EMAIL));
        when(regionRepository.findById(99L)).thenReturn(Optional.empty());

        assertError(ErrorCode.REGION_NOT_FOUND, () -> service.signUpDonor(signUp(EMAIL, "01012345678", 99L,
                List.of(TermsType.SERVICE, TermsType.PRIVACY))));
    }

    @Test
    void signUpRejectsDuplicateEmailWithoutConsumingToken() {
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.of(EMAIL));
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertError(ErrorCode.AUTH_EMAIL_DUPLICATED, () -> service.signUpDonor(validSignUp()));
        verify(emailVerificationStore, never()).consumeToken(anyString());
    }

    @Test
    void signUpMapsUniqueViolationToDuplicateEmail() {
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.of(EMAIL));
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("UQ"));

        assertError(ErrorCode.AUTH_EMAIL_DUPLICATED, () -> service.signUpDonor(validSignUp()));
    }

    @Test
    void signUpOrganizationSavesUserOrganizationTermsAndLinksVerificationHistory() {
        OrganizationVerification history = verificationHistory();
        givenVerifiedOrganization(history);
        when(passwordEncoder.encode("abcd1234")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 2L);
            return user;
        });
        when(organizationRepository.saveAndFlush(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accessTokenProvider.issue(2L, UserRole.ORGANIZATION)).thenReturn("access");
        when(refreshTokenService.issue(any(User.class), eq(false))).thenReturn("refresh");
        when(kakaoLocalClient.geocode(ORG_ADDRESS))
                .thenReturn(Optional.of(new GeoPoint(new BigDecimal("37.4930158"), new BigDecimal("126.8868294"))));

        AuthTokenResponse response = service.signUpOrganization(orgSignUp(" Org@Example.com ", "123-45-67890",
                " 김수현 ", "0212345678", "2층", ORG_TERMS));

        assertEquals(new AuthTokenResponse(2L, UserRole.ORGANIZATION, "access", "refresh"), response);
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(user.capture());
        assertEquals(ORG_EMAIL, user.getValue().getEmail());
        assertEquals("hashed", user.getValue().getPassword());
        assertEquals(UserRole.ORGANIZATION, user.getValue().getRole());
        Organization organization = savedOrganization();
        assertSame(user.getValue(), organization.getUser());
        assertEquals("구로지역아동센터", organization.getName());
        assertEquals("김수현", organization.getRepresentativeName());
        assertEquals("1234567890", organization.getBusinessNumber());
        assertEquals(OPENING_DATE, organization.getOpeningDate());
        assertEquals("02-1234-5678", organization.getPhone());
        assertEquals("서울 구로구 가마산로 12", organization.getAddress());
        assertEquals("2층", organization.getAddressDetail());
        assertEquals(new BigDecimal("37.4930158"), organization.getLatitude());
        assertEquals(new BigDecimal("126.8868294"), organization.getLongitude());
        assertEquals(history.getCheckedAt(), organization.getVerifiedAt());
        assertSame(organization, history.getOrganization());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TermsAgreement>> terms = ArgumentCaptor.forClass(List.class);
        verify(termsAgreementRepository).saveAll(terms.capture());
        assertEquals(ORG_TERMS, terms.getValue().stream().map(TermsAgreement::getTermsType).toList());
        verify(emailVerificationStore).consumeToken("verify-token");
        verify(businessVerificationStore).consume("business-token");
        // 카카오 호출 동안 DB 트랜잭션을 잡지 않는다
        InOrder order = inOrder(kakaoLocalClient, transactionManager);
        order.verify(kakaoLocalClient).geocode(ORG_ADDRESS);
        order.verify(transactionManager).getTransaction(any());
    }

    @Test
    void signUpOrganizationSucceedsWithoutCoordinatesWhenGeocodingFails() {
        givenVerifiedOrganization(verificationHistory());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationRepository.saveAndFlush(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(kakaoLocalClient.geocode(ORG_ADDRESS)).thenThrow(new KakaoApiException("down", null));

        service.signUpOrganization(validOrgSignUp());

        Organization organization = savedOrganization();
        assertNull(organization.getLatitude());
        assertNull(organization.getLongitude());
    }

    @Test
    void signUpOrganizationKeepsAreaCodeAndDropsBlankAddressDetail() {
        givenVerifiedOrganization(verificationHistory());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationRepository.saveAndFlush(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.signUpOrganization(orgSignUp(ORG_EMAIL, "1234567890", "김수현", "021234567", " ", ORG_TERMS));
        service.signUpOrganization(orgSignUp(ORG_EMAIL, "1234567890", "김수현", "0311234567", null, ORG_TERMS));
        service.signUpOrganization(orgSignUp(ORG_EMAIL, "1234567890", "김수현", "1588-1234", null, ORG_TERMS));

        ArgumentCaptor<Organization> organizations = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository, times(3)).saveAndFlush(organizations.capture());
        assertEquals(List.of("02-123-4567", "031-123-4567", "1588-1234"),
                organizations.getAllValues().stream().map(Organization::getPhone).toList());
        assertNull(organizations.getAllValues().getFirst().getAddressDetail());
    }

    @Test
    void signUpOrganizationRequiresBusinessVerificationOfSameBusiness() {
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.of(ORG_EMAIL));
        when(businessVerificationStore.peek("business-token")).thenReturn(Optional.empty());
        assertError(ErrorCode.AUTH_BUSINESS_NOT_VERIFIED, () -> service.signUpOrganization(validOrgSignUp()));

        when(businessVerificationStore.peek("business-token")).thenReturn(Optional.of(VERIFIED_BUSINESS));
        assertError(ErrorCode.AUTH_BUSINESS_NOT_VERIFIED, () -> service.signUpOrganization(
                orgSignUp(ORG_EMAIL, "1234567890", "김수연", "0212345678", null, ORG_TERMS)));
        assertError(ErrorCode.AUTH_BUSINESS_NOT_VERIFIED, () -> service.signUpOrganization(
                orgSignUp(ORG_EMAIL, "1234567891", "김수현", "0212345678", null, ORG_TERMS)));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void signUpOrganizationRequiresEmailVerification() {
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.empty());
        when(businessVerificationStore.peek("business-token")).thenReturn(Optional.of(VERIFIED_BUSINESS));

        assertError(ErrorCode.AUTH_EMAIL_NOT_VERIFIED, () -> service.signUpOrganization(validOrgSignUp()));
    }

    @Test
    void signUpOrganizationRequiresEligibilityTerms() {
        givenVerifiedOrganization(verificationHistory());

        assertError(ErrorCode.AUTH_REQUIRED_TERMS_NOT_AGREED, () -> service.signUpOrganization(orgSignUp(
                ORG_EMAIL, "1234567890", "김수현", "0212345678", null, List.of(TermsType.SERVICE, TermsType.PRIVACY))));
    }

    @Test
    void signUpOrganizationRejectsRegisteredBusinessNumberWithoutConsumingTokens() {
        givenVerifiedOrganization(verificationHistory());
        when(organizationRepository.existsByBusinessNumber("1234567890")).thenReturn(true);

        assertError(ErrorCode.AUTH_BUSINESS_NUMBER_DUPLICATED, () -> service.signUpOrganization(validOrgSignUp()));
        verify(emailVerificationStore, never()).consumeToken(anyString());
        verify(businessVerificationStore, never()).consume(anyString());
        verifyNoInteractions(kakaoLocalClient);
    }

    @Test
    void signUpOrganizationMapsUniqueViolationToDuplicateBusinessNumber() {
        givenVerifiedOrganization(verificationHistory());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationRepository.saveAndFlush(any(Organization.class)))
                .thenThrow(new DataIntegrityViolationException("UQ"));

        assertError(ErrorCode.AUTH_BUSINESS_NUMBER_DUPLICATED, () -> service.signUpOrganization(validOrgSignUp()));
    }

    @Test
    void loginIssuesTokensAndCleansUpExpiredRefreshTokens() {
        User user = user(1L, true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("abcd1234", "hashed")).thenReturn(true);
        when(accessTokenProvider.issue(1L, UserRole.DONOR)).thenReturn("access");
        when(refreshTokenService.issue(user, true)).thenReturn("refresh");

        assertEquals(new AuthTokenResponse(1L, UserRole.DONOR, "access", "refresh"),
                service.login(new LoginRequest(" Donor@Example.com ", "abcd1234")));
    }

    @Test
    void loginRejectsUnknownEmailWrongPasswordAndInactiveUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertError(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> service.login(new LoginRequest(EMAIL, "abcd1234")));

        User active = user(1L, true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        assertError(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> service.login(new LoginRequest(EMAIL, "wrong")));

        User withdrawn = user(2L, false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(withdrawn));
        when(passwordEncoder.matches("abcd1234", "hashed")).thenReturn(true);
        assertError(ErrorCode.AUTH_INVALID_CREDENTIALS, () -> service.login(new LoginRequest(EMAIL, "abcd1234")));
    }

    @Test
    void refreshRotatesAndIssuesNewTokens() {
        User user = user(1L, true);
        when(refreshTokenService.rotate("old")).thenReturn(user);
        when(accessTokenProvider.issue(1L, UserRole.DONOR)).thenReturn("new-access");
        when(refreshTokenService.issue(user, false)).thenReturn("new-refresh");

        assertEquals(new AuthTokenResponse(1L, UserRole.DONOR, "new-access", "new-refresh"),
                service.refresh(new RefreshTokenRequest("old")));
    }

    @Test
    void logoutRevokesRefreshToken() {
        service.logout(new LogoutRequest("old"));

        verify(refreshTokenService).revoke("old");
    }

    private static SignUpDonorRequest validSignUp() {
        return signUp(EMAIL, "01012345678", null, List.of(TermsType.SERVICE, TermsType.PRIVACY));
    }

    private static SignUpDonorRequest signUp(String email, String phone, Long regionId, List<TermsType> terms) {
        return new SignUpDonorRequest("verify-token", email, "abcd1234", "최윤서", LocalDate.of(1999, 3, 2),
                phone, regionId, terms);
    }

    private void givenVerifiedOrganization(OrganizationVerification history) {
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.of(ORG_EMAIL));
        when(businessVerificationStore.peek("business-token")).thenReturn(Optional.of(VERIFIED_BUSINESS));
        when(organizationVerificationRepository.findById(7L)).thenReturn(Optional.of(history));
    }

    private Organization savedOrganization() {
        ArgumentCaptor<Organization> organization = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).saveAndFlush(organization.capture());
        return organization.getValue();
    }

    private static OrganizationVerification verificationHistory() {
        return OrganizationVerification.builder()
                .verificationType(VerificationType.VALIDATE)
                .businessNumber("1234567890")
                .validCode("01")
                .businessStatusCode("01")
                .result(VerificationResult.SUCCESS)
                .checkedAt(LocalDateTime.of(2026, 10, 31, 10, 0))
                .build();
    }

    private static SignUpOrganizationRequest validOrgSignUp() {
        return orgSignUp(ORG_EMAIL, "1234567890", "김수현", "0212345678", null, ORG_TERMS);
    }

    private static SignUpOrganizationRequest orgSignUp(String email, String businessNumber, String representativeName,
                                                       String phone, String addressDetail, List<TermsType> terms) {
        return new SignUpOrganizationRequest("verify-token", "business-token", email, "abcd1234", "구로지역아동센터",
                representativeName, businessNumber, OPENING_DATE, phone, ORG_ADDRESS, addressDetail, terms);
    }

    private static User user(Long id, boolean active) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(UserRole.DONOR);
        when(user.getPassword()).thenReturn("hashed");
        when(user.isActive()).thenReturn(active);
        return user;
    }

    private static void assertError(ErrorCode expected, Executable executable) {
        BusinessException e = assertThrows(BusinessException.class, executable);
        assertEquals(expected, e.getErrorCode());
    }
}
