package com.itssum.domain.item.dto.response;

import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.NeededItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "우리 기관 필요 물품 카드 (기관 「필요 나눔 관리」)")
public record MyNeededItemResponse(
        @Schema(description = "필요 물품 ID", example = "20") Long neededItemId,
        @Schema(description = "품목", example = "생수") String itemName,
        @Schema(description = "필요 수량", example = "20") int requiredQuantity,
        @Schema(description = "예약 수량", example = "10") int reservedQuantity,
        @Schema(description = "수령 완료 수량", example = "5") int receivedQuantity,
        @Schema(description = "추가 필요 수량", example = "5") int availableQuantity,
        @Schema(description = "단위", example = "병") String unit,
        @Schema(description = "RECRUITING(모집중) | FULL(예약마감) | CLOSED(모집 종료)", example = "RECRUITING")
        NeededItemStatus status,
        @Schema(description = "카테고리 ID", example = "1") Long categoryId,
        @Schema(description = "카테고리 이름", example = "생활용품") String categoryName,
        @Schema(description = "참고 사진 조회 URL (1시간 유효). 없거나 저장소가 없는 환경에서는 null") String referenceImageUrl,
        @Schema(description = "등록 일시") LocalDateTime createdAt
) {
    public static MyNeededItemResponse of(NeededItem item, String referenceImageUrl) {
        return new MyNeededItemResponse(item.getId(), item.getItemName(), item.getRequiredQuantity(),
                item.getReservedQuantity(), item.getReceivedQuantity(), item.availableQuantity(), item.getUnit(),
                item.getStatus(), item.getCategory().getId(), item.getCategory().getName(), referenceImageUrl,
                item.getCreatedAt());
    }
}
