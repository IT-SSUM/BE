package com.itssum.domain.auth.service;

import com.itssum.domain.auth.dto.request.VerifyBusinessRequest;
import com.itssum.domain.auth.dto.response.BusinessVerificationResponse;
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
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 기관 가입 전 사업자 확인. 국세청 호출 동안 DB 트랜잭션을 잡지 않도록 트랜잭션 없이 실행하고,
 * 결과는 성공·실패와 상관없이 organization_verifications 에 남긴다 (기관 연결은 가입할 때).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessVerificationService {

    private final NtsBusinessClient ntsBusinessClient;
    private final OrganizationRepository organizationRepository;
    private final OrganizationVerificationRepository verificationRepository;
    private final BusinessVerificationStore store;
    private final SecureTokenGenerator tokenGenerator;

    public BusinessVerificationResponse verify(VerifyBusinessRequest request) {
        String businessNumber = BusinessNumberNormalizer.normalize(request.businessNumber());
        String representativeName = request.representativeName().strip();
        if (organizationRepository.existsByBusinessNumber(businessNumber)) {
            throw new BusinessException(ErrorCode.AUTH_BUSINESS_NUMBER_DUPLICATED);
        }

        NtsValidation validation;
        try {
            validation = ntsBusinessClient.validate(businessNumber, request.openingDate(), representativeName);
        } catch (NtsApiException e) {
            log.warn("국세청 진위확인 실패", e);
            saveHistory(businessNumber, null, VerificationResult.ERROR);
            throw new BusinessException(ErrorCode.AUTH_BUSINESS_VERIFICATION_UNAVAILABLE);
        }
        if (!validation.matched()) {
            saveHistory(businessNumber, validation, VerificationResult.FAIL);
            throw new BusinessException(ErrorCode.AUTH_BUSINESS_INFO_MISMATCH);
        }
        if (validation.suspendedOrClosed()) {
            saveHistory(businessNumber, validation, VerificationResult.FAIL);
            throw new BusinessException(ErrorCode.AUTH_BUSINESS_CLOSED);
        }

        OrganizationVerification history = saveHistory(businessNumber, validation, VerificationResult.SUCCESS);
        String token = tokenGenerator.generate();
        store.save(token, new VerifiedBusiness(history.getId(), businessNumber, request.openingDate(),
                representativeName));
        return new BusinessVerificationResponse(token);
    }

    private OrganizationVerification saveHistory(String businessNumber, NtsValidation validation,
                                                 VerificationResult result) {
        return verificationRepository.save(OrganizationVerification.builder()
                .verificationType(VerificationType.VALIDATE)
                .businessNumber(businessNumber)
                .validCode(validation == null ? null : validation.validCode())
                .businessStatusCode(validation == null ? null : validation.businessStatusCode())
                .taxType(validation == null ? null : validation.taxType())
                .closedDate(validation == null ? null : validation.closedDate())
                .result(result)
                .checkedAt(LocalDateTime.now())
                .build());
    }
}
