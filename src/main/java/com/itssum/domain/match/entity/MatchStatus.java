package com.itssum.domain.match.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * PENDING ─상대 수락→ TRADING(수량 미확보) ─기부자 예약→ RESERVED(수량 확보)
 * ─기관 수령 확인→ COMPLETED. PENDING ─상대 거절→ REJECTED.
 * TRADING/RESERVED ─양측 취소→ CANCELED.
 */
@Getter
@RequiredArgsConstructor
public enum MatchStatus {

    PENDING("응답 대기: 상대방 확인 전"),
    TRADING("거래중: 전달 전 협의"),
    RESERVED("예약중: 임시 수량 확보"),
    COMPLETED("전달 완료: 기관 수령 확인"),
    REJECTED("거절"),
    CANCELED("취소");

    private final String description;
}
