package com.itssum.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * organization_verifications.result
 */
@Getter
@RequiredArgsConstructor
public enum VerificationResult {

    SUCCESS("성공"),
    FAIL("실패"),
    ERROR("API 장애");

    private final String description;
}
