package com.itssum.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

    private static final String EMAIL = "donor@example.com";
    private static final AuthProperties PROPERTIES = new AuthProperties("v1", new AuthProperties.EmailVerification(
            Duration.ofMinutes(5), Duration.ofSeconds(60), 5, Duration.ofMinutes(30)),
            new AuthProperties.BusinessVerification(Duration.ofMinutes(30)));

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DonorRepository donorRepository = mock(DonorRepository.class);
    private final TermsAgreementRepository termsAgreementRepository = mock(TermsAgreementRepository.class);
    private final RegionRepository regionRepository = mock(RegionRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AccessTokenProvider accessTokenProvider = mock(AccessTokenProvider.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final EmailVerificationStore emailVerificationStore = mock(EmailVerificationStore.class);
    private final AuthService service = new AuthService(userRepository, donorRepository, termsAgreementRepository,
            regionRepository, passwordEncoder, accessTokenProvider, refreshTokenService, emailVerificationStore,
            PROPERTIES);

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
    void signUpWithoutRegionAndTenDigitPhone() {
        when(emailVerificationStore.peekVerifiedEmail("verify-token")).thenReturn(Optional.of(EMAIL));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.signUpDonor(signUp(EMAIL, "0111234567", null, List.of(TermsType.SERVICE, TermsType.PRIVACY)));

        ArgumentCaptor<Donor> donor = ArgumentCaptor.forClass(Donor.class);
        verify(donorRepository).save(donor.capture());
        assertNull(donor.getValue().getRegion());
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
