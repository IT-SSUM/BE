package com.itssum.domain.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RecommendationScorerTest {

    @Test
    void planExampleScores() {
        assertEquals(new BigDecimal("96.00"), RecommendationScorer.score(20, 20, 2.0));
        assertEquals(new BigDecimal("78.00"), RecommendationScorer.score(20, 30, 1.0));
        assertEquals(new BigDecimal("78.00"), RecommendationScorer.score(20, 25, 5.0));
    }

    @Test
    void missingDistanceScoresQuantityOnly() {
        assertEquals(new BigDecimal("60.00"), RecommendationScorer.score(20, 20, null));
    }

    @Test
    void distanceAtOrBeyondLimitScoresZero() {
        assertEquals(new BigDecimal("60.00"), RecommendationScorer.score(20, 20, 20.0));
        assertEquals(new BigDecimal("60.00"), RecommendationScorer.score(20, 20, 25.0));
    }

    @Test
    void quantityFitIsSymmetric() {
        assertEquals(new BigDecimal("40.00"), RecommendationScorer.score(20, 30, null));
        assertEquals(new BigDecimal("40.00"), RecommendationScorer.score(30, 20, null));
    }

    @Test
    void distanceAlongMeridian() {
        Double km = RecommendationScorer.distanceKm(new BigDecimal("37.5"), new BigDecimal("126.7"),
                new BigDecimal("37.6"), new BigDecimal("126.7"));

        assertEquals(11.12, km, 0.01);
    }

    @Test
    void distanceNeedsAllCoordinates() {
        BigDecimal lat = new BigDecimal("37.5");
        BigDecimal lng = new BigDecimal("126.7");

        assertNull(RecommendationScorer.distanceKm(null, lng, lat, lng));
        assertNull(RecommendationScorer.distanceKm(lat, null, lat, lng));
        assertNull(RecommendationScorer.distanceKm(lat, lng, null, lng));
        assertNull(RecommendationScorer.distanceKm(lat, lng, lat, null));
        assertNull(RecommendationScorer.roundDistance(null));
        assertEquals(new BigDecimal("1.00"), RecommendationScorer.roundDistance(1.004));
    }
}
