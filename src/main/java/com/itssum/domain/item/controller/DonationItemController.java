package com.itssum.domain.item.controller;

import com.itssum.domain.item.dto.request.CreateDonationItemRequest;
import com.itssum.domain.item.dto.request.UpdateDonationItemRequest;
import com.itssum.domain.item.dto.response.DonationItemDetailResponse;
import com.itssum.domain.item.dto.response.DonationItemSummaryResponse;
import com.itssum.domain.item.dto.response.ItemPageResponse;
import com.itssum.domain.item.dto.response.MyDonationItemResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.service.DonationItemCommandService;
import com.itssum.domain.item.service.DonationItemQueryService;
import com.itssum.domain.item.service.ItemSearchCondition;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기부 물품 API. 등록·수정·삭제와 내 목록은 기부자 전용이고, 역할 제한은 SecurityConfig 경로 규칙으로 건다.
 * 탐색·상세는 로그인한 회원 누구나 볼 수 있다. 소유자 확인은 서비스에서 한다.
 */
@Tag(name = "물품", description = "기부 물품·필요 물품 API")
@RestController
@RequiredArgsConstructor
public class DonationItemController {

    private final DonationItemCommandService commandService;
    private final DonationItemQueryService queryService;

    @Operation(summary = "기부 물품 등록",
            description = "기부자 「기부 물품 등록」 1/2 → 2/3 → 3/3 을 한 번에 보냅니다. 기부자만 호출할 수 있습니다.\n"
                    + "- 단위는 GET /api/item-units 목록의 값만 받습니다 (아니면 ITEM_400).\n"
                    + "- 사진은 업로드 URL로 올린 내 경로의 key 1~5장입니다 (확인 실패 ITEM_400_1, 저장소 미설정 ITEM_503).\n"
                    + "- 물품 위치는 좌표로 바꿔 거리 계산에 쓰며, 변환에 실패해도 등록은 됩니다.\n"
                    + "- 지역은 경기 부천시로 고정입니다.")
    @PostMapping("/api/donation-items")
    public ApiResponse<DonationItemDetailResponse> create(@Valid @RequestBody CreateDonationItemRequest request,
                                                          Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_DONATION_CREATED, commandService.create(userId(principal), request));
    }

    @Operation(summary = "기부 물품 탐색",
            description = "기관 「기부 물품 탐색」 목록입니다. 지금 신청할 수 있는 물품(모집중)만 최신 등록순으로 나옵니다. "
                    + "로그인한 회원이면 누구나 호출할 수 있습니다. 검색어는 품목 또는 기부자 이름에 포함된 글자로 찾습니다.")
    @GetMapping("/api/donation-items")
    public ApiResponse<ItemPageResponse<DonationItemSummaryResponse>> browse(
            @Parameter(description = "검색어 (품목 또는 기부자 이름)", example = "생수") @RequestParam(required = false) String keyword,
            @Parameter(description = "카테고리 ID", example = "1") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "지역 ID", example = "1") @RequestParam(required = false) Long regionId,
            @Parameter(description = "물품 상태 (이 값과 같은 물품)", example = "HIGH")
            @RequestParam(required = false) ConditionGrade condition,
            @Parameter(description = "이 ID보다 이전에 등록된 물품을 조회합니다 (이전 응답의 nextCursor)")
            @RequestParam(required = false) Long beforeId,
            @Parameter(description = "조회 개수(1~50, 기본 20)", example = "20") @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_DONATION_LIST_FOUND, queryService.browse(userId(principal),
                new ItemSearchCondition(keyword, categoryId, regionId, condition), beforeId, size));
    }

    @Operation(summary = "기부 물품 상세 조회",
            description = "기관 「기부 물품 상세」, 기부자 「물품 상세」입니다. 로그인한 회원이면 누구나 볼 수 있고 삭제된 물품은 ITEM_404 입니다. "
                    + "물품 위치·상세 주소·사진 key 는 등록한 기부자(mine = true)에게만 줍니다.")
    @GetMapping("/api/donation-items/{donationItemId}")
    public ApiResponse<DonationItemDetailResponse> detail(@PathVariable Long donationItemId, Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_DONATION_FOUND, queryService.detail(userId(principal), donationItemId));
    }

    @Operation(summary = "내가 등록한 기부 물품 목록 조회",
            description = "기부자 「등록한 물품」입니다. 상태와 관계없이 삭제하지 않은 내 물품이 최신 등록순으로 나옵니다. 기부자만 호출할 수 있습니다.")
    @GetMapping("/api/donors/me/donation-items")
    public ApiResponse<ItemPageResponse<MyDonationItemResponse>> mine(
            @Parameter(description = "카테고리 ID (전체면 생략)", example = "1") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "이 ID보다 이전에 등록된 물품을 조회합니다 (이전 응답의 nextCursor)")
            @RequestParam(required = false) Long beforeId,
            @Parameter(description = "조회 개수(1~50, 기본 20)", example = "20") @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_MY_DONATIONS_FOUND,
                queryService.mine(userId(principal), categoryId, beforeId, size));
    }

    @Operation(summary = "기부 물품 수정",
            description = "기부자 「물품 수정」 화면의 항목(설명, 물품 위치, 상세 주소, 사진)만 바꿉니다. 보내지 않은 필드는 그대로 둡니다. "
                    + "등록한 기부자만 고칠 수 있습니다 (남의 물품 ITEM_403, 삭제된 물품 ITEM_404). "
                    + "카테고리·품목·수량·단위·물품 상태는 진행 중인 거래와 추천의 기준이라 바꿀 수 없습니다.")
    @PatchMapping("/api/donation-items/{donationItemId}")
    public ApiResponse<DonationItemDetailResponse> update(@PathVariable Long donationItemId,
                                                          @Valid @RequestBody UpdateDonationItemRequest request,
                                                          Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_DONATION_UPDATED,
                commandService.update(userId(principal), donationItemId, request));
    }

    @Operation(summary = "기부 물품 삭제",
            description = "등록한 기부자만 지울 수 있습니다. 응답 대기 중인 제안·요청이나 거래중·예약중인 거래가 있으면 ITEM_409 입니다 "
                    + "(Figma \"예약된 물품은 삭제할 수 없어요\").")
    @DeleteMapping("/api/donation-items/{donationItemId}")
    public ApiResponse<Void> delete(@PathVariable Long donationItemId, Principal principal) {
        commandService.delete(userId(principal), donationItemId);
        return ApiResponse.ok(SuccessCode.ITEM_DONATION_DELETED);
    }

    private static Long userId(Principal principal) {
        return Long.valueOf(principal.getName());
    }
}
