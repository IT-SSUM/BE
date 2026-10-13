package com.itssum.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    DONOR("기부자"),
    ORGANIZATION("복지기관");

    private final String description;
}
