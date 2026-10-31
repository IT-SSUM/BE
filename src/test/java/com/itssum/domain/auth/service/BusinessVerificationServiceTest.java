package com.itssum.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itssum.domain.auth.config.AuthProperties;
import com.itssum.domain.auth.dto.request.VerifyBusinessRequest;
import com.itssum.domain.user.entity.OrganizationVerification;
import com.itssum.domain.user.entity.VerificationResult;
import com.itssum.domain.user.entity.VerificationType;
import com.itssum.domain.user.repository.OrganizationRepository;
import com.itssum.domain.user.repository.OrganizationVerificationRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.nts.NtsApiException;
import com.itssum.global.infra.nts.NtsBusinessClient;
import com.itssum.global.infra.nts.NtsValidation;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class BusinessVerificationServiceTest {

    private static final String BUSINESS_NUMBER = "1234567890";
    private static final LocalDate OPENING_DATE = LocalDate.of(2015, 3, 2);
    private static final AuthProperties PROPERTIES = new AuthProperties("v1", new AuthProperties.EmailVerification(
            Duration.ofMinutes(5), Duration.ofSeconds(60), 5, Duration.ofMinutes(30)),
            new AuthProperties.BusinessVerification(Duration.ofMinutes(30)));

    private final NtsBusinessClient ntsBusinessClient = mock(NtsBusinessClient.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final OrganizationVerificationRepository verificationRepository =
            mock(OrganizationVerificationRepository.class);
    private final BusinessVerificationStore store = new BusinessVerificationStore(PROPERTIES, new FakeTicker());
    private final SecureTokenGenerator tokenGenerator = mock(SecureTokenGenerator.class);
    private final BusinessVerificationService service = new BusinessVerificationService(
            ntsBusinessClient, organizationRepository, verificationRepository, store, tokenGenerator);

    @BeforeEach
    void setUp() {
        when(tokenGenerator.generate()).thenReturn("business-token");
        when(verificationRepository.save(any(OrganizationVerification.class))).thenAnswer(invocation -> {
            OrganizationVerification verification = invocation.getArgument(0);
            ReflectionTestUtils.setField(verification, "id", 7L);
            return verification;
        });
    }

    @Test
    void verifySavesSuccessHistoryAndIssuesTokenBoundToBusiness() {
        when(ntsBusinessClient.validate(BUSINESS_NUMBER, OPENING_DATE, "김수현"))
                .thenReturn(new NtsValidation("01", "01", "부가가치세 일반과세자", null));

        String token = service.verify(new VerifyBusinessRequest("123-45-67890", OPENING_DATE, " 김수현 "))
                .businessVerificationToken();

        assertEquals("business-token", token);
        assertEquals(Optional.of(new VerifiedBusiness(7L, BUSINESS_NUMBER, OPENING_DATE, "김수현")),
                store.peek("business-token"));
        OrganizationVerification history = savedHistory();
        assertNull(history.getOrganization());
        assertEquals(VerificationType.VALIDATE, history.getVerificationType());
        assertEquals(BUSINESS_NUMBER, history.getBusinessNumber());
        assertEquals("01", history.getValidCode());
        assertEquals("01", history.getBusinessStatusCode());
        assertEquals("부가가치세 일반과세자", history.getTaxType());
        assertEquals(VerificationResult.SUCCESS, history.getResult());
        assertNotNull(history.getCheckedAt());
    }

    @Test
    void verifyAcceptsBlankStatusCodeOfNonProfitBody() {
        when(ntsBusinessClient.validate(BUSINESS_NUMBER, OPENING_DATE, "김수현"))
                .thenReturn(new NtsValidation("01", "", "고유번호가 부여된 단체", null));

        service.verify(new VerifyBusinessRequest(BUSINESS_NUMBER, OPENING_DATE, "김수현"));

        assertEquals(VerificationResult.SUCCESS, savedHistory().getResult());
        assertTrue(store.peek("business-token").isPresent());
    }

    @Test
    void mismatchSavesFailHistoryWithoutToken() {
        when(ntsBusinessClient.validate(BUSINESS_NUMBER, OPENING_DATE, "김수현"))
                .thenReturn(new NtsValidation("02", null, null, null));

        assertError(ErrorCode.AUTH_BUSINESS_INFO_MISMATCH,
                () -> service.verify(new VerifyBusinessRequest(BUSINESS_NUMBER, OPENING_DATE, "김수현")));

        OrganizationVerification history = savedHistory();
        assertEquals("02", history.getValidCode());
        assertEquals(VerificationResult.FAIL, history.getResult());
        assertTrue(store.peek("business-token").isEmpty());
    }

    @Test
    void suspendedOrClosedBusinessSavesFailHistory() {
        when(ntsBusinessClient.validate(BUSINESS_NUMBER, OPENING_DATE, "김수현"))
                .thenReturn(new NtsValidation("01", "03", "부가가치세 간이과세자", LocalDate.of(2024, 1, 31)));

        assertError(ErrorCode.AUTH_BUSINESS_CLOSED,
                () -> service.verify(new VerifyBusinessRequest(BUSINESS_NUMBER, OPENING_DATE, "김수현")));

        OrganizationVerification history = savedHistory();
        assertEquals("03", history.getBusinessStatusCode());
        assertEquals(LocalDate.of(2024, 1, 31), history.getClosedDate());
        assertEquals(VerificationResult.FAIL, history.getResult());
        assertTrue(store.peek("business-token").isEmpty());
    }

    @Test
    void registeredBusinessNumberIsRejectedBeforeCallingNts() {
        when(organizationRepository.existsByBusinessNumber(BUSINESS_NUMBER)).thenReturn(true);

        assertError(ErrorCode.AUTH_BUSINESS_NUMBER_DUPLICATED,
                () -> service.verify(new VerifyBusinessRequest("123-45-67890", OPENING_DATE, "김수현")));

        verifyNoInteractions(ntsBusinessClient);
        verify(verificationRepository, never()).save(any());
    }

    @Test
    void ntsFailureSavesErrorHistory() {
        when(ntsBusinessClient.validate(BUSINESS_NUMBER, OPENING_DATE, "김수현"))
                .thenThrow(new NtsApiException("down"));

        assertError(ErrorCode.AUTH_BUSINESS_VERIFICATION_UNAVAILABLE,
                () -> service.verify(new VerifyBusinessRequest(BUSINESS_NUMBER, OPENING_DATE, "김수현")));

        OrganizationVerification history = savedHistory();
        assertNull(history.getValidCode());
        assertEquals(VerificationResult.ERROR, history.getResult());
        assertTrue(store.peek("business-token").isEmpty());
    }

    private OrganizationVerification savedHistory() {
        ArgumentCaptor<OrganizationVerification> history = ArgumentCaptor.forClass(OrganizationVerification.class);
        verify(verificationRepository).save(history.capture());
        return history.getValue();
    }

    private static void assertError(ErrorCode expected, Executable executable) {
        BusinessException e = assertThrows(BusinessException.class, executable);
        assertEquals(expected, e.getErrorCode());
    }
}
