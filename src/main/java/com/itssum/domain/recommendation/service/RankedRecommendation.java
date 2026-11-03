package com.itssum.domain.recommendation.service;

import com.itssum.domain.item.entity.NeededItem;
import java.math.BigDecimal;

/** 순위가 매겨진 추천 후보. 총점과 거리는 소수 둘째 자리로 반올림한 값이다. */
public record RankedRecommendation(int rank, NeededItem neededItem, BigDecimal score, BigDecimal distanceKm) {
}
