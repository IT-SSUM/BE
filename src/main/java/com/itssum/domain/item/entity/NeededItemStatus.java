package com.itssum.domain.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NeededItemStatus {

    RECRUITING("모집중"),
    FULL("신규 제안 제한: 추가 필요 0"),
    CLOSED("모집 종료");

    private final String description;
}
