package com.itssum.domain.chat.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {

    TEXT("텍스트"),
    IMAGE("이미지"),
    SYSTEM("나눔 연결·나눔중 변경·수령 확인 안내"),
    APPOINTMENT("전달 약속 카드: matches 일정·장소·방법 참조");

    private final String description;
}
