package com.itssum.domain.recommendation.service;

import com.itssum.domain.item.entity.NeededItem;
import java.math.BigDecimal;

/** 추천 계산 시점의 후보 조건과 지원 가능 수량으로 화면에 표시할 이유를 만든다. */
final class RecommendationReason {

    private RecommendationReason() {
    }

    static String from(int donationAvailableQuantity, RankedRecommendation ranked) {
        NeededItem needed = ranked.neededItem();
        int required = needed.availableQuantity();
        int supported = Math.min(donationAvailableQuantity, required);
        String unit = needed.getUnit();

        String quantity = supported == required
                ? "필요 수량 " + required + unit + " 전체를 지원할 수 있어요."
                : "필요 수량 " + required + unit + " 중 " + supported + unit + "까지 지원할 수 있어요.";
        String reason = "요청한 물품 분류와 상태에 맞아요. " + quantity;
        BigDecimal distance = ranked.distanceKm();
        if (distance != null) {
            reason += distance.signum() == 0
                    ? " 기관과 매우 가까운 위치예요."
                    : " 기관까지 직선거리 약 " + distance.stripTrailingZeros().toPlainString() + "km예요.";
        }
        return reason;
    }
}
