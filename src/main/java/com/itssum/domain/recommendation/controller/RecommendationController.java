package com.itssum.domain.recommendation.controller;

import com.itssum.domain.recommendation.dto.response.OrganizationRecommendationResponse;
import com.itssum.domain.recommendation.service.RecommendationService;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기부자 「기관 추천」 API. 기부자만 호출할 수 있고(SecurityConfig 경로 규칙), 내 물품인지는 서비스에서 확인한다.
 */
@Tag(name = "추천", description = "규칙 기반 기관 추천 API")
@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private static final String COMMON_DESCRIPTION = "\n- 기부자만 호출할 수 있습니다 (기관 COMMON_403, 남의 물품 RECOMMENDATION_403, "
            + "없거나 삭제된 물품 ITEM_404).\n"
            + "- 같은 카테고리·단위, 희망 상태 충족, 인증된 기관, 모집 중, 진행 중인 거래 없음을 만족하는 필요 물품을 "
            + "총점(수량 적합도 60% + 거리 40%) 순으로 기관당 1개, 최대 3개 돌려줍니다.\n"
            + "- 신청 가능 수량이 0인 물품은 빈 목록입니다.\n"
            + "- reason은 계산 당시의 물품 조건·지원 가능 수량·거리를 설명하며, 좌표가 없으면 거리 설명은 제외합니다.";

    private final RecommendationService recommendationService;

    @Operation(summary = "추천 기관 계산",
            description = "기부자 「기관 추천」에 [추천 기관 보기]로 들어올 때 부릅니다. 다시 계산해 저장하고, "
                    + "이전 결과를 지우고 추천 이유도 새로 생성합니다." + COMMON_DESCRIPTION)
    @PostMapping("/api/donation-items/{donationItemId}/recommendations")
    public ApiResponse<List<OrganizationRecommendationResponse>> recommend(@PathVariable Long donationItemId,
                                                                           Principal principal) {
        return ApiResponse.ok(SuccessCode.RECOMMENDATION_CREATED,
                recommendationService.recommend(userId(principal), donationItemId));
    }

    @Operation(summary = "추천 기관 조회",
            description = "기부자 「기관 추천」을 다시 볼 때 부릅니다. 저장된 결과에서 지금 제안할 수 있는 기관만 순위를 다시 매겨 "
                    + "돌려주고, 계산한 적이 없으면 빈 목록입니다." + COMMON_DESCRIPTION)
    @GetMapping("/api/donation-items/{donationItemId}/recommendations")
    public ApiResponse<List<OrganizationRecommendationResponse>> find(@PathVariable Long donationItemId,
                                                                      Principal principal) {
        return ApiResponse.ok(SuccessCode.RECOMMENDATION_FOUND,
                recommendationService.find(userId(principal), donationItemId));
    }

    private static Long userId(Principal principal) {
        return Long.valueOf(principal.getName());
    }
}
