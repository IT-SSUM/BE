package com.itssum.global.infra.kakao;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 카카오 로컬 API (주소 → 좌표). REST API 키는 환경변수 KAKAO_REST_API_KEY 로만 받는다.
 */
@ConfigurationProperties("kakao")
public record KakaoProperties(String baseUrl, String restApiKey, Duration timeout) {
}
