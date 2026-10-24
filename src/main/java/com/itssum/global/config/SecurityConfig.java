package com.itssum.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 최소 설정. JWT 인증 필터와 경로별 인가 규칙은 다음 작업(global.security)에서 추가한다.
 */
@Configuration
public class SecurityConfig {

    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CD 배포 헬스체크. JWT 적용 후에도 인증 없이 열어 둔다.
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        // 기준 데이터 조회는 비로그인 상태(회원가입·둘러보기)에서도 쓴다
                        .requestMatchers(HttpMethod.GET, "/api/item-conditions").permitAll()
                        // TODO: JWT 적용 시 인증이 필요한 경로를 제한한다. 지금은 전체 허용.
                        .anyRequest().permitAll());
        return http.build();
    }
}
