package com.itssum.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    MATCH_REQUESTED("새 제안·요청"),
    MATCH_RESERVED("수락"),
    MATCH_REJECTED("거절"),
    MATCH_SCHEDULED("전달 일정 확정"),
    MATCH_SHARING("전달 준비 시작"),
    MATCH_RECEIVE_REQUIRED("수령 확인 요청"),
    MATCH_COMPLETED("전달 완료"),
    MATCH_CANCELED("취소·수량 복원"),
    CHAT("채팅");

    private final String description;
}
