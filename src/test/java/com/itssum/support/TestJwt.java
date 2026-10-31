package com.itssum.support;

/** @WebMvcTest 전용 JWT 서명 키. 운영·로컬 키와 무관하다. */
public final class TestJwt {

    public static final String SECRET = "test-jwt-secret-for-web-mvc-tests-0123456789";

    private TestJwt() {
    }
}
