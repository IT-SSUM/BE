package com.itssum.global.config;

import com.itssum.global.security.JwtConfig;
import com.itssum.global.security.PublicPathBearerTokenResolver;
import com.itssum.global.security.RestSecurityErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * JWT(Bearer) 인증과 경로별 인가 규칙. 역할별 제한은 각 도메인 API 를 만들 때 붙인다.
 */
@Configuration
@Import(JwtConfig.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    /** 인증 없이 호출하는 인증 API (POST). /api/auth/** 로 한꺼번에 열지 않고 하나씩 적는다. */
    public static final String[] PUBLIC_AUTH_PATHS = {
            "/api/auth/email-verifications",
            "/api/auth/email-verifications/confirm",
            "/api/auth/donors/sign-up",
            "/api/auth/login",
            "/api/auth/tokens/refresh",
            "/api/auth/logout"
    };

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final JsonMapper jsonMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RestSecurityErrorHandler errorHandler = new RestSecurityErrorHandler(jsonMapper);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CD 배포 헬스체크
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        // 기준 데이터 조회는 비로그인 상태(회원가입·둘러보기)에서도 쓴다
                        .requestMatchers(HttpMethod.GET, "/api/item-conditions").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_PATHS).permitAll()
                        // WebSocket 핸드셰이크. 인증은 STOMP CONNECT 에서 한다 (StompAuthenticationInterceptor)
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new PublicPathBearerTokenResolver(PUBLIC_AUTH_PATHS))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
