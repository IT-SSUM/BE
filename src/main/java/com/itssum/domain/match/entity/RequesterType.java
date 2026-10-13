package com.itssum.domain.match.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * matches.requested_by: 매칭을 먼저 요청한 쪽
 */
@Getter
@RequiredArgsConstructor
public enum RequesterType {

    DONOR("기부 제안"),
    ORGANIZATION("나눔 요청");

    private final String description;
}
