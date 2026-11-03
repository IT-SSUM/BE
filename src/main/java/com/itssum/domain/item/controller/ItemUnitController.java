package com.itssum.domain.item.controller;

import com.itssum.domain.item.entity.ItemUnits;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "물품", description = "기부 물품·필요 물품 API")
@RestController
@RequestMapping("/api/item-units")
public class ItemUnitController {

    @Operation(
            summary = "물품 단위 목록 조회",
            description = """
                    물품 등록의 수량 칸에서 고르는 단위 목록을 노출 순서대로 반환합니다.
                    - 등록 요청의 unit 에는 이 목록의 값을 그대로 보냅니다. 목록에 없는 단위는 ITEM_400 입니다.
                    - 매칭은 단위가 같은 물품끼리만 됩니다.
                    - 로그인 없이 호출할 수 있습니다.
                    """
    )
    @SecurityRequirements // 기준 데이터라 인증 불필요 → 전역 JWT 요구사항을 이 API에서 해제
    @GetMapping
    public ApiResponse<List<String>> getItemUnits() {
        return ApiResponse.ok(SuccessCode.ITEM_UNITS_FOUND, ItemUnits.ALL);
    }
}
