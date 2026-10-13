package com.itssum.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * notifications.target_type: 알림을 눌렀을 때 이동할 대상
 */
@Getter
@RequiredArgsConstructor
public enum NotificationTargetType {

    MATCH("매칭"),
    CHAT_ROOM("채팅방"),
    DONATION_ITEM("기부 물품"),
    NEEDED_ITEM("필요 물품");

    private final String description;
}
