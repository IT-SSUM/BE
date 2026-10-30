package com.itssum.domain.auth.controller;

import com.itssum.domain.auth.dto.request.ConfirmEmailVerificationRequest;
import com.itssum.domain.auth.dto.request.LoginRequest;
import com.itssum.domain.auth.dto.request.LogoutRequest;
import com.itssum.domain.auth.dto.request.RefreshTokenRequest;
import com.itssum.domain.auth.dto.request.SendEmailVerificationRequest;
import com.itssum.domain.auth.dto.request.SignUpDonorRequest;
import com.itssum.domain.auth.dto.response.AuthTokenResponse;
import com.itssum.domain.auth.dto.response.EmailVerificationConfirmResponse;
import com.itssum.domain.auth.dto.response.EmailVerificationSendResponse;
import com.itssum.domain.auth.service.AuthService;
import com.itssum.domain.auth.service.EmailVerificationService;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.common.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 없이 호출하는 API. 경로를 바꾸면 SecurityConfig.PUBLIC_AUTH_PATHS 도 같이 바꾼다.
 */
@Tag(name = "인증", description = "이메일 인증·회원가입·로그인·토큰 API")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;

    @Operation(summary = "이메일 인증번호 발송",
            description = "6자리 인증번호를 메일로 보냅니다. 유효시간 5분, 재발송은 60초 뒤부터 가능합니다. 이미 가입된 이메일이면 AUTH_409.")
    @PostMapping("/email-verifications")
    public ApiResponse<EmailVerificationSendResponse> sendEmailVerification(
            @Valid @RequestBody SendEmailVerificationRequest request) {
        return ApiResponse.ok(SuccessCode.AUTH_EMAIL_VERIFICATION_SENT, emailVerificationService.send(request));
    }

    @Operation(summary = "이메일 인증번호 확인",
            description = "맞으면 회원가입 요청에 넣을 emailVerificationToken(30분, 1회용)을 돌려줍니다. 5번 틀리면 번호가 폐기됩니다.")
    @PostMapping("/email-verifications/confirm")
    public ApiResponse<EmailVerificationConfirmResponse> confirmEmailVerification(
            @Valid @RequestBody ConfirmEmailVerificationRequest request) {
        return ApiResponse.ok(SuccessCode.AUTH_EMAIL_VERIFIED, emailVerificationService.confirm(request));
    }

    @Operation(summary = "기부자 회원가입",
            description = "가입과 동시에 로그인 토큰을 돌려줍니다. 필수 약관(SERVICE, PRIVACY) 동의와 이메일 인증 토큰이 필요합니다.")
    @PostMapping("/donors/sign-up")
    public ApiResponse<AuthTokenResponse> signUpDonor(@Valid @RequestBody SignUpDonorRequest request) {
        return ApiResponse.ok(SuccessCode.AUTH_SIGN_UP, authService.signUpDonor(request));
    }

    @Operation(summary = "로그인", description = "기부자·기관 공통입니다. 응답의 role 로 홈 화면을 고릅니다.")
    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(SuccessCode.AUTH_LOGIN, authService.login(request));
    }

    @Operation(summary = "토큰 재발급",
            description = "새 액세스·리프레시 토큰을 돌려줍니다. 쓴 리프레시 토큰은 바로 무효가 되므로 재발급은 한 번에 하나씩 요청합니다.")
    @PostMapping("/tokens/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(SuccessCode.AUTH_TOKEN_REFRESHED, authService.refresh(request));
    }

    @Operation(summary = "로그아웃",
            description = "이 기기의 리프레시 토큰을 폐기합니다. 액세스 토큰 없이 호출하며, 앱은 두 토큰을 모두 지웁니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.ok(SuccessCode.AUTH_LOGOUT);
    }
}
