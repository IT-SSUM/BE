package com.itssum.domain.match.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * REQUESTED ─수락→ RESERVED ─기부자 "나눔중으로 변경"→ SHARING ─기관 "수령 확인"→ COMPLETED
 * REQUESTED ─거절→ REJECTED / 진행 중 취소 → CANCELED (수량 복원)
 */
@Getter
@RequiredArgsConstructor
public enum MatchStatus {

    REQUESTED("응답 대기"),
    RESERVED("예약중: 수락·수량 확보"),
    SHARING("나눔중: 기부자 전달 시작"),
    COMPLETED("전달 완료: 기관 수령 확인"),
    REJECTED("거절"),
    CANCELED("취소");

    private final String description;
}
