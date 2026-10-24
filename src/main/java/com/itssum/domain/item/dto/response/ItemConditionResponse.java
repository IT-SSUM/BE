package com.itssum.domain.item.dto.response;

import com.itssum.domain.item.entity.ConditionGrade;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "물품 상태 기준")
public record ItemConditionResponse(
        @Schema(description = "상태 코드. 기부 물품 등록·필요 물품 등록 요청에 이 값을 보낸다.", example = "HIGH")
        ConditionGrade code,

        @Schema(description = "화면 표시용 설명", example = "상·미개봉")
        String description
) {

    public static ItemConditionResponse from(ConditionGrade grade) {
        return new ItemConditionResponse(grade, grade.getDescription());
    }
}
