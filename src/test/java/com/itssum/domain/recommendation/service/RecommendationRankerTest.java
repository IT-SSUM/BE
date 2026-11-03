package com.itssum.domain.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itssum.domain.item.ItemFixtures;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.user.entity.Organization;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RecommendationRankerTest {

    private static final LocalDateTime REGISTERED = LocalDateTime.of(2026, 11, 3, 10, 0);

    @Test
    void planExampleRanksACB() {
        NeededItem a = needed(1L, 101L, 20, REGISTERED);
        NeededItem b = needed(2L, 102L, 25, REGISTERED);
        NeededItem c = needed(3L, 103L, 30, REGISTERED);

        List<RankedRecommendation> ranked = RecommendationRanker.rank(20, List.of(
                new RecommendationCandidate(a, 2.0),
                new RecommendationCandidate(b, 5.0),
                new RecommendationCandidate(c, 1.0)));

        assertEquals(List.of(a, c, b), ranked.stream().map(RankedRecommendation::neededItem).toList());
        assertEquals(List.of(1, 2, 3), ranked.stream().map(RankedRecommendation::rank).toList());
        assertEquals(List.of(new BigDecimal("96.00"), new BigDecimal("78.00"), new BigDecimal("78.00")),
                ranked.stream().map(RankedRecommendation::score).toList());
        assertEquals(List.of(new BigDecimal("2.00"), new BigDecimal("1.00"), new BigDecimal("5.00")),
                ranked.stream().map(RankedRecommendation::distanceKm).toList());
    }

    @Test
    void unknownDistanceGoesLastOnTie() {
        NeededItem unknown = needed(1L, 101L, 20, REGISTERED);
        NeededItem known = needed(2L, 102L, 12, REGISTERED);

        List<RankedRecommendation> ranked = RecommendationRanker.rank(20, List.of(
                new RecommendationCandidate(unknown, null),
                new RecommendationCandidate(known, 8.0)));

        assertEquals(List.of(known, unknown), ranked.stream().map(RankedRecommendation::neededItem).toList());
        assertEquals(List.of(new BigDecimal("60.00"), new BigDecimal("60.00")),
                ranked.stream().map(RankedRecommendation::score).toList());
    }

    @Test
    void tieBreaksByCreatedAtThenId() {
        NeededItem later = needed(1L, 101L, 20, REGISTERED.plusHours(1));
        NeededItem earlierHigherId = needed(3L, 103L, 20, REGISTERED);
        NeededItem earlierLowerId = needed(2L, 102L, 20, REGISTERED);

        List<RankedRecommendation> ranked = RecommendationRanker.rank(20, List.of(
                new RecommendationCandidate(later, 3.0),
                new RecommendationCandidate(earlierHigherId, 3.0),
                new RecommendationCandidate(earlierLowerId, 3.0)));

        assertEquals(List.of(earlierLowerId, earlierHigherId, later),
                ranked.stream().map(RankedRecommendation::neededItem).toList());
    }

    @Test
    void keepsBestPerOrganization() {
        Organization organization = ItemFixtures.organization(101L);
        NeededItem weaker = needed(1L, organization, 40, REGISTERED);
        NeededItem stronger = needed(2L, organization, 20, REGISTERED);

        List<RankedRecommendation> ranked = RecommendationRanker.rank(20, List.of(
                new RecommendationCandidate(weaker, 1.0),
                new RecommendationCandidate(stronger, 1.0)));

        assertEquals(List.of(stronger), ranked.stream().map(RankedRecommendation::neededItem).toList());
        assertEquals(1, ranked.get(0).rank());
    }

    @Test
    void keepsTopThree() {
        NeededItem first = needed(1L, 101L, 20, REGISTERED);
        NeededItem second = needed(2L, 102L, 20, REGISTERED);
        NeededItem third = needed(3L, 103L, 20, REGISTERED);
        NeededItem fourth = needed(4L, 104L, 20, REGISTERED);

        List<RankedRecommendation> ranked = RecommendationRanker.rank(20, List.of(
                new RecommendationCandidate(fourth, 4.0),
                new RecommendationCandidate(second, 2.0),
                new RecommendationCandidate(third, 3.0),
                new RecommendationCandidate(first, 1.0)));

        assertEquals(List.of(first, second, third), ranked.stream().map(RankedRecommendation::neededItem).toList());
    }

    @Test
    void emptyCandidatesGiveEmpty() {
        assertTrue(RecommendationRanker.rank(20, List.of()).isEmpty());
    }

    private static NeededItem needed(Long id, Long organizationId, int remaining, LocalDateTime createdAt) {
        return needed(id, ItemFixtures.organization(organizationId), remaining, createdAt);
    }

    private static NeededItem needed(Long id, Organization organization, int remaining, LocalDateTime createdAt) {
        NeededItem item = ItemFixtures.neededItem(id, organization);
        ReflectionTestUtils.setField(item, "requiredQuantity", remaining);
        ReflectionTestUtils.setField(item, "createdAt", createdAt);
        return item;
    }
}
