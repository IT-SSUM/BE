package com.itssum.global.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itssum.domain.user.entity.UserRole;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

class AccessTokenProviderTest {

    private static final String SECRET = "test-jwt-secret-0123456789-abcdefghijklmnop";

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void issuedTokenCarriesUserIdRoleAndExpiry() {
        JwtProperties properties = properties(SECRET, Duration.ofMinutes(30));
        AccessTokenProvider provider = new AccessTokenProvider(jwtConfig.jwtEncoder(properties), properties);

        Jwt jwt = jwtConfig.jwtDecoder(properties).decode(provider.issue(7L, UserRole.DONOR));

        assertEquals("7", jwt.getSubject());
        assertEquals("DONOR", jwt.getClaimAsString(AccessTokenProvider.ROLE_CLAIM));
        assertEquals(Duration.ofMinutes(30), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    void roleClaimBecomesRoleAuthorityAndNameIsUserId() {
        JwtProperties properties = properties(SECRET, Duration.ofMinutes(30));
        AccessTokenProvider provider = new AccessTokenProvider(jwtConfig.jwtEncoder(properties), properties);
        Jwt jwt = jwtConfig.jwtDecoder(properties).decode(provider.issue(7L, UserRole.ORGANIZATION));

        AbstractAuthenticationToken authentication = jwtConfig.jwtAuthenticationConverter().convert(jwt);

        assertEquals("7", authentication.getName());
        // Spring Security 7 은 인증 수단을 나타내는 FACTOR_BEARER 권한도 함께 붙인다
        List<String> authorities =
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(authorities.contains("ROLE_ORGANIZATION"));
        assertFalse(authorities.contains("ROLE_DONOR"));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtProperties properties = properties(SECRET, Duration.ofMinutes(30));
        String token = new AccessTokenProvider(jwtConfig.jwtEncoder(properties), properties).issue(7L, UserRole.DONOR);
        JwtDecoder otherKeyDecoder = jwtConfig.jwtDecoder(
                properties("another-secret-0123456789-abcdefghijklmnop", Duration.ofMinutes(30)));

        assertThrows(JwtException.class, () -> otherKeyDecoder.decode(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtProperties properties = properties(SECRET, Duration.ofMinutes(30));
        // 인코더는 exp < iat 를 거부하므로 과거 시각으로 직접 서명한다. 시계 오차 허용(60초)보다 더 지난 토큰이다
        Instant issuedAt = Instant.now().minus(Duration.ofMinutes(40));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("7")
                .claim(AccessTokenProvider.ROLE_CLAIM, "DONOR")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofMinutes(30)))
                .build();
        String token = jwtConfig.jwtEncoder(properties)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThrows(JwtException.class, () -> jwtConfig.jwtDecoder(properties).decode(token));
    }

    @Test
    void secretShorterThan32BytesFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtProperties("too-short", Duration.ofMinutes(30), Duration.ofDays(14)));
    }

    private static JwtProperties properties(String secret, Duration accessTokenTtl) {
        return new JwtProperties(secret, accessTokenTtl, Duration.ofDays(14));
    }
}
