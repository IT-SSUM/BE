package com.itssum.domain.item.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "물품 목록 한 페이지 (최신 등록순)")
public record ItemPageResponse<T>(
        List<T> items,
        @Schema(description = "다음 조회의 beforeId. 더 오래된 물품이 없으면 null", example = "103") Long nextCursor,
        @Schema(description = "더 오래된 물품 존재 여부", example = "true") boolean hasNext
) {
}
