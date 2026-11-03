package com.itssum.domain.match.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * PENDING ─상대 수락→ TRADING(수량 미확보) ─기부자 예약→ RESERVED(수량 확보)
 * ─기관 수령 확인→ COMPLETED. PENDING ─상대 거절→ REJECTED.
 * PENDING ─발신자 철회→ CANCELED. TRADING/RESERVED ─양측 취소→ CANCELED.
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

    /**
     * 끝난 매칭: 전달 완료·거절·응답 대기 철회·거래 취소. 이 밖의 상태(응답 대기 중인 제안·요청, 거래중, 예약중)는 진행 중으로 본다.
     * 진행 중 상태의 이름이 바뀌어도 규칙이 그대로 맞도록 끝난 상태로 판단한다 (회원 탈퇴·물품 삭제 검사).
     */
    public static final Set<MatchStatus> FINISHED =
            Collections.unmodifiableSet(EnumSet.of(COMPLETED, REJECTED, CANCELED));

    private final String description;
}
