package com.itssum.domain.item.dto.response;

import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.DonationItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "내가 등록한 기부 물품 카드 (기부자 「등록한 물품」)")
public record MyDonationItemResponse(
        @Schema(description = "기부 물품 ID", example = "10") Long donationItemId,
        @Schema(description = "품목", example = "수건") String itemName,
        @Schema(description = "총 수량", example = "30") int quantity,
        @Schema(description = "예약 수량", example = "10") int reservedQuantity,
        @Schema(description = "전달 완료 수량", example = "0") int completedQuantity,
        @Schema(description = "신청 가능 수량", example = "20") int availableQuantity,
        @Schema(description = "단위", example = "장") String unit,
        @Schema(description = "RECRUITING(모집중) | FULL(예약마감) | COMPLETED(전달 완료)", example = "RECRUITING")
        DonationItemStatus status,
        @Schema(description = "카테고리 ID", example = "1") Long categoryId,
        @Schema(description = "카테고리 이름", example = "생활용품") String categoryName,
        @Schema(description = "대표 사진 조회 URL (1시간 유효). 저장소가 없는 환경에서는 null") String imageUrl,
        @Schema(description = "등록 일시") LocalDateTime createdAt
) {
    public static MyDonationItemResponse of(DonationItem item, String imageUrl) {
        return new MyDonationItemResponse(item.getId(), item.getItemName(), item.getQuantity(), item.getReservedQuantity(),
                item.getCompletedQuantity(), item.availableQuantity(), item.getUnit(), item.getStatus(),
                item.getCategory().getId(), item.getCategory().getName(), imageUrl, item.getCreatedAt());
    }
}
