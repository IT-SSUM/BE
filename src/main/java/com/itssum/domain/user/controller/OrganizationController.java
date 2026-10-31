package com.itssum.domain.user.controller;

import com.itssum.domain.user.dto.request.UpdateOrganizationProfileRequest;
import com.itssum.domain.user.dto.response.OrganizationDetailResponse;
import com.itssum.domain.user.dto.response.OrganizationProfileResponse;
import com.itssum.domain.user.service.OrganizationProfileService;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /me 는 기관 전용이다. 역할 제한은 SecurityConfig 경로 규칙(/api/organizations/me → ORGANIZATION)으로 건다.
 * /{organizationId} 는 로그인한 회원 누구나 볼 수 있다.
 */
@Tag(name = "회원", description = "내 정보 조회·수정, 기관 상세, 회원 탈퇴 API")
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationProfileService organizationProfileService;

    @Operation(summary = "기관 정보 조회", description = "마이페이지 헤더와 기관 정보 수정 화면의 기존 값입니다. 기관만 호출할 수 있습니다.")
    @GetMapping("/me")
    public ApiResponse<OrganizationProfileResponse> getMe(Principal principal) {
        return ApiResponse.ok(SuccessCode.USER_ORGANIZATION_PROFILE_FOUND,
                organizationProfileService.getMine(userId(principal)));
    }

    @Operation(summary = "기관 정보 수정",
            description = "기관명·연락처·주소·상세주소를 바꿉니다. 보내지 않은 필드는 그대로 둡니다. 주소를 보내면 상세주소도 함께 바뀌고, "
                    + "주소가 바뀌면 좌표를 다시 계산합니다. 대표자명·사업자번호·개업일자는 바꿀 수 없습니다.")
    @PatchMapping("/me")
    public ApiResponse<OrganizationProfileResponse> updateMe(Principal principal,
                                                             @Valid @RequestBody UpdateOrganizationProfileRequest request) {
        return ApiResponse.ok(SuccessCode.USER_ORGANIZATION_PROFILE_UPDATED,
                organizationProfileService.updateMine(userId(principal), request));
    }

    @Operation(summary = "기관 상세 조회",
            description = "기부자가 추천·탐색에서 들어가는 기관 상세의 기관 정보입니다. 로그인한 회원이면 누구나 볼 수 있고, "
                    + "탈퇴했거나 없는 기관이면 USER_404 입니다.")
    @GetMapping("/{organizationId}")
    public ApiResponse<OrganizationDetailResponse> getDetail(Principal principal, @PathVariable Long organizationId) {
        return ApiResponse.ok(SuccessCode.USER_ORGANIZATION_DETAIL_FOUND,
                organizationProfileService.getDetail(userId(principal), organizationId));
    }

    private static Long userId(Principal principal) {
        return Long.valueOf(principal.getName());
    }
}
