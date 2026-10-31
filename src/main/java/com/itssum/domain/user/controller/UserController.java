package com.itssum.domain.user.controller;

import com.itssum.domain.user.service.WithdrawalService;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원", description = "내 정보 조회·수정, 기관 상세, 회원 탈퇴 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final WithdrawalService withdrawalService;

    @Operation(summary = "회원 탈퇴",
            description = "기부자·기관 공통입니다. 응답 대기 중인 기부 제안·기부 요청이나 거래중·예약중인 거래가 있으면 USER_409 로 막습니다. "
                    + "탈퇴하면 모든 기기에서 로그아웃되고 개인정보가 지워지며, 같은 이메일·사업자번호로 다시 가입할 수 있습니다. "
                    + "앱은 갖고 있던 두 토큰을 모두 지웁니다.")
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(Principal principal) {
        withdrawalService.withdraw(Long.valueOf(principal.getName()));
        return ApiResponse.ok(SuccessCode.USER_WITHDRAWN);
    }
}
