package com.itssum.domain.item.dto.request;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.global.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "기부 물품 등록 요청 (Figma 1/2 물품 정보 → 2/3 상태·사진 → 3/3 위치를 한 번에 보낸다)")
public record CreateDonationItemRequest(
        @Schema(description = "카테고리 ID (GET /api/item-categories)", example = "5")
        @NotNull(message = "카테고리를 선택해 주세요.")
        Long categoryId,
        @Schema(description = "품목. 목록·상세의 제목으로 쓴다", example = "어린이 도서")
        @NotNull(message = "품목을 입력해 주세요.") @Size(max = 50) @NullOrNotBlank(message = "품목을 입력해 주세요.")
        String itemName,
        @Schema(description = "총 수량", example = "30")
        @NotNull(message = "수량을 입력해 주세요.") @Min(value = 1, message = "수량은 1 이상으로 입력해 주세요.")
        Integer quantity,
        @Schema(description = "단위 (GET /api/item-units 목록의 값)", example = "권")
        @NotNull(message = "단위를 선택해 주세요.")
        String unit,
        @Schema(description = "물품 상태 (GET /api/item-conditions)", example = "MID")
        @NotNull(message = "물품 상태를 선택해 주세요.")
        ConditionGrade conditionGrade,
        @Schema(description = "설명 (선택)", example = "초등학생이 읽기 좋은 도서예요.")
        @Size(max = 1000)
        String description,
        @Schema(description = "사진 key 1~5장 (POST /api/donation-items/images/presign 으로 올린 objectKey). 첫 번째가 대표 사진",
                example = "[\"donation-items/1/0f8c2a8e-7c1b-4d2e-9a51-3f6b2c1d4e5f.jpg\"]")
        @NotEmpty(message = "사진을 1장 이상 올려 주세요.") @Size(max = 5, message = "사진은 5장까지 올릴 수 있습니다.")
        List<@NotBlank String> imageKeys,
        @Schema(description = "물품 위치 (주소 검색 결과의 도로명 주소). 좌표로 바꿔 거리 계산에 쓰고, 다른 회원에게는 구 단위 지역만 보인다",
                example = "경기 부천시 길주로 210")
        @NotNull(message = "물품 위치를 입력해 주세요.") @Size(max = 255) @NullOrNotBlank(message = "물품 위치를 입력해 주세요.")
        String pickupAddress,
        @Schema(description = "상세 주소 (선택, 동·호수나 전달 메모)", example = "건물 입구에서 전달할게요")
        @Size(max = 100)
        String pickupAddressDetail,
        @Schema(description = "지역 ID (선택). 생략하면 경기 부천시(1)", example = "1")
        Long regionId
) {
}
