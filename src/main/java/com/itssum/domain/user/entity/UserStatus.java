package com.itssum.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {

    ACTIVE("활성"),
    WITHDRAWN("탈퇴");

    private final String description;
}
