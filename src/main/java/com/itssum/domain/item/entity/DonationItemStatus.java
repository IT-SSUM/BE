package com.itssum.domain.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DonationItemStatus {

    SHARING("나눔중: 가능 수량 남음"),
    RESERVED("예약중: 남은 수량 0"),
    COMPLETED("전달 완료");

    private final String description;
}
