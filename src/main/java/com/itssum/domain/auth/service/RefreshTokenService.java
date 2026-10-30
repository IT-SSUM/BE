package com.itssum.domain.auth.service;

import com.itssum.domain.auth.entity.RefreshToken;
import com.itssum.domain.auth.repository.RefreshTokenRepository;
import com.itssum.domain.user.entity.User;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.security.JwtProperties;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리프레시 토큰은 기기(로그인)마다 1행이고, 재발급할 때마다 지우고 새로 발급한다(rotation).
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final JwtProperties jwtProperties;

    /** 새 토큰을 저장하고 원문을 돌려준다. 로그인일 때는 그 회원의 만료된 토큰을 먼저 지운다. */
    @Transactional
    public String issue(User user, boolean cleanUpExpired) {
        LocalDateTime now = LocalDateTime.now();
        if (cleanUpExpired) {
            refreshTokenRepository.deleteExpiredByUserId(user.getId(), now);
        }
        String raw = tokenGenerator.generate();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(tokenGenerator.hash(raw))
                .expiresAt(now.plus(jwtProperties.refreshTokenTtl()))
                .build());
        return raw;
    }

    /**
     * 토큰을 소모하고 회원을 돌려준다. 새 토큰 발급은 호출한 쪽이 한다.
     * 만료된 행은 여기서 지우지 않는다 (예외로 롤백되므로). 다음 로그인 때 정리된다.
     */
    @Transactional
    public User rotate(String rawToken) {
        RefreshToken token = refreshTokenRepository.findWithUserByTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(RefreshTokenService::invalid);
        if (token.isExpired(LocalDateTime.now())) {
            throw invalid();
        }
        // 같은 토큰으로 동시에 들어오면 행 잠금 때문에 한 요청만 1행을 지운다
        if (refreshTokenRepository.deleteByIdAndReturnCount(token.getId()) == 0) {
            throw invalid();
        }
        User user = token.getUser();
        if (!user.isActive()) {
            throw invalid();
        }
        return user;
    }

    /** 로그아웃. 이미 없는 토큰이어도 성공으로 본다. */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.deleteByTokenHash(tokenGenerator.hash(rawToken));
    }

    private static BusinessException invalid() {
        return new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }
}
