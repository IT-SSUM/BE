package com.itssum.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * organization_verifications.verification_type
 */
@Getter
@RequiredArgsConstructor
public enum VerificationType {

    VALIDATE("진위확인"),
    STATUS("상태조회");

    private final String description;
}
