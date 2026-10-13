package com.itssum.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 메인 클래스가 아닌 별도 설정에 두어 @WebMvcTest 같은 슬라이스 테스트에서 JPA 빈을 요구하지 않게 한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
