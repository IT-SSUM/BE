package com.itssum.domain.auth.service;

import java.time.LocalDate;

/**
 * 국세청 진위확인을 통과한 사업자 정보. 사업자 확인 토큰에 묶어 두고, 가입 요청의 값과 같은지 본다.
 *
 * @param verificationId 진위확인 이력(organization_verifications) ID. 가입하면 기관과 연결한다.
 */
public record VerifiedBusiness(Long verificationId, String businessNumber, LocalDate openingDate,
                               String representativeName) {

    public boolean matches(String businessNumber, LocalDate openingDate, String representativeName) {
        return this.businessNumber.equals(businessNumber)
                && this.openingDate.equals(openingDate)
                && this.representativeName.equals(representativeName);
    }
}
