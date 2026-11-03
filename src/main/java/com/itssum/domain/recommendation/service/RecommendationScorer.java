package com.itssum.domain.recommendation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 기획안 「08. 추천 알고리즘」의 점수 계산. 총점 = 100 × (0.6 × 수량 적합도 + 0.4 × 거리 점수).
 * 거리 기준 20km 와 가중치는 검증 전 초기값이다.
 */
public final class RecommendationScorer {

    public static final double DISTANCE_LIMIT_KM = 20.0;
    public static final double QUANTITY_WEIGHT = 0.6;
    public static final double DISTANCE_WEIGHT = 0.4;
    public static final int MAX_RESULTS = 3;
    public static final double EARTH_RADIUS_KM = 6371.0;

    private static final int SCALE = 2;

    private RecommendationScorer() {
    }

    /** 거리를 모르면(좌표 없음) 거리 점수는 0 이다. 결과는 소수 둘째 자리에서 반올림한다. */
    public static BigDecimal score(int donationQuantity, int remainingQuantity, Double distanceKm) {
        double quantityFit = (double) Math.min(donationQuantity, remainingQuantity)
                / Math.max(donationQuantity, remainingQuantity);
        double distanceScore = distanceKm == null ? 0 : Math.max(0, 1 - distanceKm / DISTANCE_LIMIT_KM);
        double total = 100 * (QUANTITY_WEIGHT * quantityFit + DISTANCE_WEIGHT * distanceScore);
        return BigDecimal.valueOf(total).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 두 좌표 사이 대권거리(km, 하버사인). 하나라도 없으면 null. */
    public static Double distanceKm(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }
        double phi1 = Math.toRadians(lat1.doubleValue());
        double phi2 = Math.toRadians(lat2.doubleValue());
        double deltaPhi = phi2 - phi1;
        double deltaLambda = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.pow(Math.sin(deltaPhi / 2), 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(deltaLambda / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    public static BigDecimal roundDistance(Double distanceKm) {
        return distanceKm == null ? null : BigDecimal.valueOf(distanceKm).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
