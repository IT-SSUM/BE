package com.itssum.domain.user.controller;

import com.itssum.domain.user.dto.request.UpdateDonorProfileRequest;
import com.itssum.domain.user.dto.response.DonorProfileResponse;
import com.itssum.domain.user.service.DonorProfileService;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.common.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기부자 전용. 역할 제한은 SecurityConfig 경로 규칙(/api/donors/me → DONOR)으로 건다.
 */
@Tag(name = "회원", description = "내 정보 조회·수정, 기관 상세, 회원 탈퇴 API")
@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private final DonorProfileService donorProfileService;

    @Operation(summary = "기부자 프로필 조회", description = "마이페이지 헤더와 회원정보 수정 화면의 기존 값입니다. 기부자만 호출할 수 있습니다.")
    @GetMapping("/me")
    public ApiResponse<DonorProfileResponse> getMe(Principal principal) {
        return ApiResponse.ok(SuccessCode.USER_DONOR_PROFILE_FOUND, donorProfileService.get(userId(principal)));
    }

    @Operation(summary = "기부자 프로필 수정",
            description = "이름·생년월일·전화번호를 바꿉니다. 보내지 않은 필드는 그대로 둡니다. 이메일·활동 지역은 바꿀 수 없습니다.")
    @PatchMapping("/me")
    public ApiResponse<DonorProfileResponse> updateMe(Principal principal,
                                                      @Valid @RequestBody UpdateDonorProfileRequest request) {
        return ApiResponse.ok(SuccessCode.USER_DONOR_PROFILE_UPDATED,
                donorProfileService.update(userId(principal), request));
    }

    private static Long userId(Principal principal) {
        return Long.valueOf(principal.getName());
    }
}
