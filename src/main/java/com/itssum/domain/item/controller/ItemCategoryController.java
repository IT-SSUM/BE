package com.itssum.domain.item.controller;

import com.itssum.domain.item.dto.response.ItemCategoryResponse;
import com.itssum.domain.item.repository.ItemCategoryRepository;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "물품", description = "기부 물품·필요 물품 API")
@RestController
@RequestMapping("/api/item-categories")
@RequiredArgsConstructor
public class ItemCategoryController {

    private final ItemCategoryRepository itemCategoryRepository;

    @Operation(
            summary = "물품 카테고리 목록 조회",
            description = """
                    물품 등록의 「카테고리 선택」 화면과 탐색 필터에 쓰는 카테고리 목록을 노출 순서대로 반환합니다.
                    - 추천은 같은 카테고리의 물품끼리 묶습니다.
                    - 로그인 없이 호출할 수 있습니다.
                    """
    )
    @SecurityRequirements // 기준 데이터라 인증 불필요 → 전역 JWT 요구사항을 이 API에서 해제
    @GetMapping
    public ApiResponse<List<ItemCategoryResponse>> getItemCategories() {
        List<ItemCategoryResponse> categories = itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(ItemCategoryResponse::from)
                .toList();
        return ApiResponse.ok(SuccessCode.ITEM_CATEGORIES_FOUND, categories);
    }
}
