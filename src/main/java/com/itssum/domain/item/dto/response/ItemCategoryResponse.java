package com.itssum.domain.item.dto.response;

import com.itssum.domain.item.entity.ItemCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "물품 카테고리")
public record ItemCategoryResponse(
        @Schema(description = "카테고리 ID. 물품 등록 요청과 목록 필터에 이 값을 보낸다.", example = "5")
        Long categoryId,
        @Schema(description = "카테고리 이름", example = "도서·문구")
        String name
) {
    public static ItemCategoryResponse from(ItemCategory category) {
        return new ItemCategoryResponse(category.getId(), category.getName());
    }
}
