package com.itssum.domain.item.dto.response;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.NeededItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "필요 물품 탐색 카드 (기부자 「물품 탐색」)")
public record NeededItemSummaryResponse(
        @Schema(description = "필요 물품 ID", example = "20") Long neededItemId,
        @Schema(description = "품목", example = "생수") String itemName,
        @Schema(description = "추가 필요 수량", example = "20") int availableQuantity,
        @Schema(description = "단위", example = "병") String unit,
        @Schema(description = "기관명", example = "구로지역아동센터") String organizationName,
        @Schema(description = "수령 지역 표시용 이름", example = "경기 부천시") String regionName,
        @Schema(description = "카테고리 이름", example = "생활용품") String categoryName,
        @Schema(description = "희망 물품 상태 (이 등급 이상)", example = "HIGH") ConditionGrade desiredCondition,
        @Schema(description = "참고 사진 조회 URL (1시간 유효). 없거나 저장소가 없는 환경에서는 null") String referenceImageUrl,
        @Schema(description = "등록 일시") LocalDateTime createdAt
) {
    public static NeededItemSummaryResponse of(NeededItem item, String referenceImageUrl) {
        return new NeededItemSummaryResponse(item.getId(), item.getItemName(), item.availableQuantity(), item.getUnit(),
                item.getOrganization().getName(), item.getRegion().displayName(), item.getCategory().getName(),
                item.getDesiredCondition(), referenceImageUrl, item.getCreatedAt());
    }
}
