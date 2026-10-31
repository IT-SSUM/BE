package com.itssum.global.infra.kakao;

import java.math.BigDecimal;

/** 위도·경도. 컬럼(DECIMAL(10,7))에 맞춰 소수 7자리로 맞춘 값이다. */
public record GeoPoint(BigDecimal latitude, BigDecimal longitude) {
}
