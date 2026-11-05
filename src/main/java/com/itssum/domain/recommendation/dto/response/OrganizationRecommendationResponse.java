package com.itssum.domain.recommendation.dto.response;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.recommendation.entity.OrganizationRecommendation;
import com.itssum.domain.user.entity.Organization;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** 「기관 추천」 카드 한 장: 추천된 기관과 그 기관의 필요 물품 하나. */
public record OrganizationRecommendationResponse(
        @Schema(description = "순위 (1부터)", example = "1") int rank,
        @Schema(description = "기관 ID (기관 상세 조회용)", example = "2") Long organizationId,
        @Schema(description = "기관명", example = "구로지역아동센터") String organizationName,
        @Schema(description = "기관 주소 (상세주소 제외)", example = "경기 부천시 길주로 210") String organizationAddress,
        @Schema(description = "필요 물품 ID (필요 물품 상세·기부 제안용)", example = "7") Long neededItemId,
        @Schema(description = "필요 물품 품목", example = "어린이 도서") String itemName,
        @Schema(description = "추가 필요 수량 (지금 값)", example = "30") int remainingQuantity,
        @Schema(description = "단위", example = "권") String unit,
        @Schema(description = "희망 물품 상태 (이 등급 이상)", example = "MID") ConditionGrade desiredCondition,
        @Schema(description = "총점 (0~100, 계산 시점 기준)", example = "96.00") BigDecimal score,
        @Schema(description = "기부 물품 위치와 기관 사이 직선 거리(km), 좌표가 없으면 null", example = "2.00")
        BigDecimal distanceKm,
        @Schema(description = "추천 계산 당시의 물품 조건·지원 가능 수량·거리로 작성한 이유. 좌표가 없으면 거리 설명 제외",
                example = "요청한 물품 분류와 상태에 맞아요. 필요 수량 30권 중 20권까지 지원할 수 있어요. 기관까지 직선거리 약 2km예요.")
        String reason) {

    public static OrganizationRecommendationResponse of(int rank, OrganizationRecommendation recommendation) {
        NeededItem neededItem = recommendation.getNeededItem();
        Organization organization = recommendation.getOrganization();
        return new OrganizationRecommendationResponse(rank, organization.getId(), organization.getName(),
                organization.getAddress(), neededItem.getId(), neededItem.getItemName(), neededItem.availableQuantity(),
                neededItem.getUnit(), neededItem.getDesiredCondition(), recommendation.getScore(),
                recommendation.getDistanceKm(), recommendation.getReason());
    }
}
