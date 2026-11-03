package com.itssum.domain.recommendation.service;

import com.itssum.domain.item.entity.NeededItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 후보의 점수를 매기고 순위를 정한다. 총점 높은 순 → 거리 가까운 순(모르면 뒤) → 필요 물품 등록 시각 이른 순 → ID 순.
 * 비교는 화면·DB 에 남는 값과 같도록 반올림한 총점·거리로 한다. 기관당 가장 높은 하나만 남기고 상위 3개를 고른다.
 */
public final class RecommendationRanker {

    private static final Comparator<Scored> ORDER = Comparator
            .comparing(Scored::score, Comparator.reverseOrder())
            .thenComparing(Scored::distanceKm, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing((Scored scored) -> scored.neededItem().getCreatedAt(),
                    Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()))
            .thenComparing(scored -> scored.neededItem().getId());

    private RecommendationRanker() {
    }

    public static List<RankedRecommendation> rank(int donationQuantity, List<RecommendationCandidate> candidates) {
        List<Scored> sorted = candidates.stream()
                .map(candidate -> new Scored(candidate.neededItem(),
                        RecommendationScorer.score(donationQuantity, candidate.neededItem().availableQuantity(),
                                candidate.distanceKm()),
                        RecommendationScorer.roundDistance(candidate.distanceKm())))
                .sorted(ORDER)
                .toList();

        Set<Long> seenOrganizations = new HashSet<>();
        List<RankedRecommendation> ranked = new ArrayList<>();
        for (Scored scored : sorted) {
            if (ranked.size() == RecommendationScorer.MAX_RESULTS) {
                break;
            }
            if (seenOrganizations.add(scored.neededItem().getOrganization().getId())) {
                ranked.add(new RankedRecommendation(ranked.size() + 1, scored.neededItem(), scored.score(),
                        scored.distanceKm()));
            }
        }
        return ranked;
    }

    private record Scored(NeededItem neededItem, BigDecimal score, BigDecimal distanceKm) {
    }
}
