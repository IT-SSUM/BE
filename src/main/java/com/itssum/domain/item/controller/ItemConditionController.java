package com.itssum.domain.item.controller;

import com.itssum.domain.item.dto.response.ItemConditionResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.common.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "물품", description = "기부 물품·필요 물품 API")
@RestController
@RequestMapping("/api/item-conditions")
public class ItemConditionController {

    @Operation(
            summary = "물품 상태 기준 조회",
            description = """
                    기부 물품의 상태(상·중·하)를 고를 때, 필요 물품의 희망 최소 상태를 고를 때 쓰는 기준 목록을 반환합니다.

                    - 상태가 좋은 순서(HIGH → MID → LOW)로 내려갑니다.
                    - 필요 물품의 희망 최소 상태는 "이 등급 이상"을 뜻합니다. (예: MID = 중 이상)
                    - 로그인 없이 호출할 수 있습니다.
                    """
    )
    @SecurityRequirements // 기준 데이터라 인증 불필요 → 전역 JWT 요구사항을 이 API에서 해제
    @GetMapping
    public ApiResponse<List<ItemConditionResponse>> getItemConditions() {
        List<ItemConditionResponse> conditions = Arrays.stream(ConditionGrade.values())
                .map(ItemConditionResponse::from)
                .toList();
        return ApiResponse.ok(SuccessCode.ITEM_CONDITIONS_FOUND, conditions);
    }
}
