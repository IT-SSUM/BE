package com.itssum.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    MATCH_REQUESTED("새 기부 제안·요청"),
    MATCH_ACCEPTED("기부 제안·요청 수락"),
    MATCH_REJECTED("기부 제안·요청 거절"),
    MATCH_RESERVED("기부자 임시 예약"),
    MATCH_SCHEDULED("전달 약속 확정·변경"),
    MATCH_RECEIVE_REQUIRED("수령 확인 요청"),
    MATCH_COMPLETED("전달 완료"),
    MATCH_CANCELED("거래 취소"),
    CHAT("채팅");

    private final String description;
}
