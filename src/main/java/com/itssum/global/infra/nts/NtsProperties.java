package com.itssum.global.infra.nts;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 국세청 사업자등록정보 진위확인·상태조회 API (공공데이터포털). serviceKey 는 디코딩 키를 환경변수 NTS_SERVICE_KEY 로만 받는다.
 */
@ConfigurationProperties("nts")
public record NtsProperties(String baseUrl, String serviceKey, Duration timeout) {
}
