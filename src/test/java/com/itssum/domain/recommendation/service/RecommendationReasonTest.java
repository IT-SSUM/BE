package com.itssum.domain.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.itssum.domain.item.ItemFixtures;
import com.itssum.domain.item.entity.NeededItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RecommendationReasonTest {

    @Test
    void partialSupportUsesActualUnitAndQuantity() {
        NeededItem needed = ItemFixtures.neededItem(1L, ItemFixtures.organization(2L));
        ReflectionTestUtils.setField(needed, "requiredQuantity", 30);
        ReflectionTestUtils.setField(needed, "unit", "권");

        String reason = RecommendationReason.from(20,
                new RankedRecommendation(1, needed, new BigDecimal("78.00"), new BigDecimal("2.00")));

        assertEquals("요청한 물품 분류와 상태에 맞아요. 필요 수량 30권 중 20권까지 지원할 수 있어요. "
                + "기관까지 직선거리 약 2km예요.", reason);
    }
}
