package com.itssum.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeviceType {

    ANDROID("안드로이드"),
    IOS("iOS"),
    WEB("웹");

    private final String description;
}
