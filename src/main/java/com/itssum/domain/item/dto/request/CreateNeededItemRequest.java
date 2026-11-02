package com.itssum.domain.item.dto.request;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.global.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "필요 물품 등록 요청 (Figma 1/2 물품·지역 → 2/2 상태·설명을 한 번에 보낸다)")
public record CreateNeededItemRequest(
        @Schema(description = "카테고리 ID (GET /api/item-categories)", example = "1")
        @NotNull(message = "카테고리를 선택해 주세요.")
        Long categoryId,
        @Schema(description = "품목. 목록·상세의 제목으로 쓴다", example = "생수")
        @NotNull(message = "품목을 입력해 주세요.") @Size(max = 50) @NullOrNotBlank(message = "품목을 입력해 주세요.")
        String itemName,
        @Schema(description = "필요 수량", example = "20")
        @NotNull(message = "필요 수량을 입력해 주세요.") @Min(value = 1, message = "수량은 1 이상으로 입력해 주세요.")
        Integer requiredQuantity,
        @Schema(description = "단위 (GET /api/item-units 목록의 값)", example = "병")
        @NotNull(message = "단위를 선택해 주세요.")
        String unit,
        @Schema(description = "수령 지역 ID (선택). 생략하면 경기 부천시(1)", example = "1")
        Long regionId,
        @Schema(description = "희망 물품 상태 (이 등급 이상). GET /api/item-conditions", example = "MID")
        @NotNull(message = "희망 물품 상태를 선택해 주세요.")
        ConditionGrade desiredCondition,
        @Schema(description = "필요한 이유 (선택)", example = "아이들이 편하게 마실 생수가 필요해요.")
        @Size(max = 1000)
        String description,
        @Schema(description = "참고 사진 key (선택, 1장). POST /api/needed-items/images/presign 으로 올린 objectKey",
                example = "needed-items/2/0f8c2a8e-7c1b-4d2e-9a51-3f6b2c1d4e5f.jpg")
        @Size(max = 500) @NullOrNotBlank
        String referenceImageKey
) {
}
