package com.itssum.support;

import com.itssum.global.config.SecurityConfig;
import com.itssum.global.security.AccessTokenProvider;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * @WebMvcTest 에서 실제 보안 설정(JWT 인가 규칙, 401·403 JSON 응답)을 쓴다.
 * jwt.secret 은 환경변수 대신 테스트 값을 넣는다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, AccessTokenProvider.class})
@TestPropertySource(properties = "jwt.secret=" + TestJwt.SECRET)
public @interface SecurityTestConfig {
}
