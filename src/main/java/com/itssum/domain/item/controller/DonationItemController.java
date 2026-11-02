package com.itssum.domain.item.controller;

import com.itssum.domain.item.dto.request.CreateDonationItemRequest;
import com.itssum.domain.item.dto.response.DonationItemDetailResponse;
import com.itssum.domain.item.service.DonationItemCommandService;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private static Long userId(Principal principal) {
        return Long.valueOf(principal.getName());
    }
}
