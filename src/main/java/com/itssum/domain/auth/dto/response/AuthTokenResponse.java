package com.itssum.domain.auth.dto.response;

import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthTokenResponse(
        @Schema(description = "회원 ID", example = "1") Long userId,
        @Schema(description = "회원 역할. 앱은 이 값으로 기부자/기관 홈을 고른다", example = "DONOR") UserRole role,
        @Schema(description = "액세스 토큰 (JWT, 30분). Authorization: Bearer 로 보낸다") String accessToken,
        @Schema(description = "리프레시 토큰 (14일, 재발급할 때마다 바뀜)") String refreshToken
) {

    public static AuthTokenResponse of(User user, String accessToken, String refreshToken) {
        return new AuthTokenResponse(user.getId(), user.getRole(), accessToken, refreshToken);
    }
}
