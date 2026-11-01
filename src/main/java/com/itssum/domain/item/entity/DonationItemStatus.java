package com.itssum.domain.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DonationItemStatus {

    RECRUITING("모집중: 신청 가능 수량 남음"),
    FULL("예약마감: 신청 가능 수량 0"),
    COMPLETED("전달 완료");

    private final String description;
}
