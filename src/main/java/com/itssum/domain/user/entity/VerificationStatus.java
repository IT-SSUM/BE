package com.itssum.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * organizations.verification_status
 */
@Getter
@RequiredArgsConstructor
public enum VerificationStatus {

    VERIFIED("인증 완료"),
    SUSPENDED("상태조회로 휴·폐업 감지");

    private final String description;
}
