package com.itssum.domain.match.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeliveryMethod {

    DIRECT("직접 전달"),
    PARCEL("택배");

    private final String description;
}
