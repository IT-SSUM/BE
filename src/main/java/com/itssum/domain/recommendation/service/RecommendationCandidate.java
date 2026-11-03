package com.itssum.domain.recommendation.service;

import com.itssum.domain.item.entity.NeededItem;

/** 조건을 통과한 필요 물품과 기부 물품까지의 거리(km, 좌표가 없으면 null). */
public record RecommendationCandidate(NeededItem neededItem, Double distanceKm) {
}
