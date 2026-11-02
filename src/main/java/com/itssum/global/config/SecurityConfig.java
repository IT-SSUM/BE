package com.itssum.global.config;

import com.itssum.global.security.JwtConfig;
import com.itssum.global.security.PublicPathBearerTokenResolver;
import com.itssum.global.security.RestSecurityErrorHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
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
            "/api/auth/organizations/business-verifications",
            "/api/auth/organizations/sign-up",
            "/api/auth/login",
            "/api/auth/tokens/refresh",
            "/api/auth/logout"
    };

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final JsonMapper jsonMapper;

    // 브라우저에서 API를 부를 수 있는 FE 출처 (application.yml cors.allowed-origins)
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RestSecurityErrorHandler errorHandler = new RestSecurityErrorHandler(jsonMapper);
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CD 배포 헬스체크
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        // 기준 데이터 조회는 비로그인 상태(회원가입·둘러보기)에서도 쓴다
                        .requestMatchers(HttpMethod.GET, "/api/item-conditions", "/api/item-categories", "/api/item-units")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_PATHS).permitAll()
                        // WebSocket 핸드셰이크. 인증은 STOMP CONNECT 에서 한다 (StompAuthenticationInterceptor)
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        // 내 정보 API 는 역할별로 막는다 (다른 역할이면 403)
                        .requestMatchers("/api/donors/me", "/api/donors/me/**").hasRole("DONOR")
                        .requestMatchers("/api/organizations/me").hasRole("ORGANIZATION")
                        // 물품 쓰기는 역할별로 막는다: 기부 물품은 기부자, 필요 물품은 기관 (소유자 확인은 서비스)
                        .requestMatchers(HttpMethod.POST, "/api/donation-items", "/api/donation-items/images/presign")
                        .hasRole("DONOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/donation-items/*").hasRole("DONOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/donation-items/*").hasRole("DONOR")
                        .requestMatchers(HttpMethod.POST, "/api/needed-items", "/api/needed-items/images/presign")
                        .hasRole("ORGANIZATION")
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

    /** 토큰은 Authorization 헤더와 요청 본문으로 주고받으므로 쿠키(자격 증명)는 허용하지 않는다. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowedOrigins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
