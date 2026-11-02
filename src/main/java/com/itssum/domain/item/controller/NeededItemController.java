package com.itssum.domain.item.controller;

import com.itssum.domain.item.dto.request.CreateNeededItemRequest;
import com.itssum.domain.item.dto.response.ItemPageResponse;
import com.itssum.domain.item.dto.response.MyNeededItemResponse;
import com.itssum.domain.item.dto.response.NeededItemDetailResponse;
import com.itssum.domain.item.dto.response.NeededItemSummaryResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.service.ItemSearchCondition;
import com.itssum.domain.item.service.NeededItemCommandService;
import com.itssum.domain.item.service.NeededItemQueryService;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 필요 물품 API. 등록·수정·삭제와 우리 기관 목록은 기관 전용이고, 역할 제한은 SecurityConfig 경로 규칙으로 건다.
 * 탐색·상세는 로그인한 회원 누구나 볼 수 있다. 소유자 확인은 서비스에서 한다.
 */
@Tag(name = "물품", description = "기부 물품·필요 물품 API")
@RestController
@RequiredArgsConstructor
public class NeededItemController {

    private final NeededItemCommandService commandService;
    private final NeededItemQueryService queryService;

    @Operation(summary = "필요 물품 등록",
            description = "기관 「필요 물품 등록」 1/2 → 2/2 를 한 번에 보냅니다. 기관만 호출할 수 있습니다.\n"
                    + "- 단위는 GET /api/item-units 목록의 값만 받습니다 (아니면 ITEM_400).\n"
                    + "- 참고 사진은 선택이며, 업로드 URL로 올린 내 경로의 key 1장입니다 (확인 실패 ITEM_400_1, 저장소 미설정 ITEM_503).\n"
                    + "- 수령 지역은 경기 부천시로 고정이고, 거리는 기관 주소 좌표로 계산합니다.")
    @PostMapping("/api/needed-items")
    public ApiResponse<NeededItemDetailResponse> create(@Valid @RequestBody CreateNeededItemRequest request,
                                                        Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_NEEDED_CREATED, commandService.create(userId(principal), request));
    }

    @Operation(summary = "필요 물품 탐색",
            description = "기부자 「물품 탐색」 목록입니다. 지금 기부할 수 있는 물품(모집중)만 최신 등록순으로 나오고, "
                    + "사업자 확인이 유효하지 않은(휴·폐업) 기관의 물품은 빠집니다. 로그인한 회원이면 누구나 호출할 수 있습니다. "
                    + "검색어는 품목 또는 기관명에 포함된 글자로 찾습니다.")
    @GetMapping("/api/needed-items")
    public ApiResponse<ItemPageResponse<NeededItemSummaryResponse>> browse(
            @Parameter(description = "검색어 (품목 또는 기관명)", example = "생수") @RequestParam(required = false) String keyword,
            @Parameter(description = "카테고리 ID", example = "1") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "수령 지역 ID", example = "1") @RequestParam(required = false) Long regionId,
            @Parameter(description = "희망 상태 (이 값과 같은 물품)", example = "MID")
            @RequestParam(required = false) ConditionGrade condition,
            @Parameter(description = "이 ID보다 이전에 등록된 물품을 조회합니다 (이전 응답의 nextCursor)")
            @RequestParam(required = false) Long beforeId,
            @Parameter(description = "조회 개수(1~50, 기본 20)", example = "20") @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_NEEDED_LIST_FOUND, queryService.browse(userId(principal),
                new ItemSearchCondition(keyword, categoryId, regionId, condition), beforeId, size));
    }

    @Operation(summary = "필요 물품 상세 조회",
            description = "기부자 「물품 상세」, 기관 「필요 나눔」입니다. 로그인한 회원이면 누구나 볼 수 있고 삭제된 물품은 ITEM_404 입니다. "
                    + "수령 장소·참고 사진 key 는 등록한 기관(mine = true)에게만 줍니다.")
    @GetMapping("/api/needed-items/{neededItemId}")
    public ApiResponse<NeededItemDetailResponse> detail(@PathVariable Long neededItemId, Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_NEEDED_FOUND, queryService.detail(userId(principal), neededItemId));
    }

    @Operation(summary = "우리 기관 필요 물품 목록 조회",
            description = "기관 「필요 나눔 관리」입니다. 상태와 관계없이 삭제하지 않은 우리 기관 물품이 최신 등록순으로 나옵니다. 기관만 호출할 수 있습니다.")
    @GetMapping("/api/organizations/me/needed-items")
    public ApiResponse<ItemPageResponse<MyNeededItemResponse>> mine(
            @Parameter(description = "카테고리 ID (전체면 생략)", example = "1") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "이 ID보다 이전에 등록된 물품을 조회합니다 (이전 응답의 nextCursor)")
            @RequestParam(required = false) Long beforeId,
            @Parameter(description = "조회 개수(1~50, 기본 20)", example = "20") @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_MY_NEEDED_FOUND,
                queryService.mine(userId(principal), categoryId, beforeId, size));
    }

    private static Long userId(Principal principal) {
        return Long.valueOf(principal.getName());
    }
}
