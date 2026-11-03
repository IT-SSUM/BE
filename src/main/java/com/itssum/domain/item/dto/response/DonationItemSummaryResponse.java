package com.itssum.domain.item.dto.response;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "기부 물품 탐색 카드 (기관 「기부 물품 탐색」)")
public record DonationItemSummaryResponse(
        @Schema(description = "기부 물품 ID", example = "10") Long donationItemId,
        @Schema(description = "품목", example = "생수") String itemName,
        @Schema(description = "신청 가능 수량", example = "20") int availableQuantity,
        @Schema(description = "단위", example = "개") String unit,
        @Schema(description = "기부자 이름", example = "최윤서") String donorName,
        @Schema(description = "물품 지역 표시용 이름", example = "경기 부천시") String regionName,
        @Schema(description = "물품 상태", example = "HIGH") ConditionGrade conditionGrade,
        @Schema(description = "대표 사진 조회 URL (1시간 유효). 저장소가 없는 환경에서는 null") String imageUrl,
        @Schema(description = "등록 일시") LocalDateTime createdAt
) {
    public static DonationItemSummaryResponse of(DonationItem item, String imageUrl) {
        return new DonationItemSummaryResponse(item.getId(), item.getItemName(), item.availableQuantity(), item.getUnit(),
                item.getDonor().getName(), item.getRegion().displayName(), item.getConditionGrade(), imageUrl,
                item.getCreatedAt());
    }
}
