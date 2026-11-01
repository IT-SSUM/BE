package com.itssum.domain.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NeededItemStatus {

    RECRUITING("모집중"),
    FULL("예약마감: 추가 필요 수량 0"),
    CLOSED("모집 종료");

    private final String description;
}
