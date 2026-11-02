package com.itssum.domain.item.dto.request;

import com.itssum.global.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "기부 물품 수정 요청 (기부자 「물품 수정」 화면의 항목만). 보내지 않은 필드는 그대로 둔다. "
        + "카테고리·품목·수량·단위·물품 상태는 등록 후 바꿀 수 없다")
public record UpdateDonationItemRequest(
        @Schema(description = "설명. 빈 값이면 비운다", example = "초등학생이 읽기 좋은 도서예요.")
        @Size(max = 1000)
        String description,
        @Schema(description = "물품 위치 (도로명 주소). 바뀌면 좌표를 다시 구한다", example = "경기 부천시 길주로 210")
        @Size(max = 255) @NullOrNotBlank(message = "물품 위치를 입력해 주세요.")
        String pickupAddress,
        @Schema(description = "상세 주소. 빈 값이면 비운다", example = "건물 입구에서 전달할게요")
        @Size(max = 100)
        String pickupAddressDetail,
        @Schema(description = "사진 key 1~5장. 보내면 사진 목록 전체를 이 순서로 바꾼다 (첫 번째가 대표 사진). "
                + "유지할 사진은 상세 응답의 imageKeys 를 그대로 넣는다")
        @Size(min = 1, max = 5, message = "사진은 1~5장이어야 합니다.")
        List<@NotBlank String> imageKeys
) {
}
