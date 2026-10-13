package com.itssum.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TermsType {

    SERVICE("서비스 이용약관"),
    PRIVACY("개인정보 수집·이용"),
    ORG_ELIGIBILITY("기관 가입 자격 확인");

    private final String description;
}
