package com.itssum.domain.item.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "필요 물품 수정 요청 (기관 「필요 나눔」 수정 화면의 항목만). 보내지 않은 필드는 그대로 두고, 빈 값이면 비운다. "
        + "카테고리·품목·수량·단위·희망 상태는 등록 후 바꿀 수 없다")
public record UpdateNeededItemRequest(
        @Schema(description = "필요한 이유", example = "평일 오후에 물품을 수령할 수 있어요.")
        @Size(max = 1000)
        String description,
        @Schema(description = "수령 장소", example = "서울 구로구 가마산로 12")
        @Size(max = 255)
        String receiveAddress,
        @Schema(description = "참고 사진 key. 유지하려면 상세 응답의 referenceImageKey 를 그대로, 지우려면 빈 값을 보낸다",
                example = "needed-items/2/0f8c2a8e-7c1b-4d2e-9a51-3f6b2c1d4e5f.jpg")
        @Size(max = 500)
        String referenceImageKey
) {
}
