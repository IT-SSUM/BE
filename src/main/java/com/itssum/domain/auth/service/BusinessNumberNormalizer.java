package com.itssum.domain.auth.service;

/** 사업자 확인과 가입에서 같은 기준으로 비교하도록 하이픈을 빼고 숫자 10자리만 남긴다 (organizations.business_number). */
final class BusinessNumberNormalizer {

    private BusinessNumberNormalizer() {
    }

    static String normalize(String businessNumber) {
        return businessNumber.replaceAll("\\D", "");
    }
}
