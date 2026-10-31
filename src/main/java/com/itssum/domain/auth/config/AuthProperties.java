package com.itssum.domain.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 회원가입·이메일 인증·사업자 확인 정책 (application.yml 의 auth.*). 비밀값이 아니라 환경변수로 빼지 않는다. */
@ConfigurationProperties("auth")
public record AuthProperties(String termsVersion, EmailVerification emailVerification,
                             BusinessVerification businessVerification) {

    public record EmailVerification(Duration codeTtl, Duration resendCooldown, int maxAttempts, Duration tokenTtl) {
    }

    public record BusinessVerification(Duration tokenTtl) {
    }
}
