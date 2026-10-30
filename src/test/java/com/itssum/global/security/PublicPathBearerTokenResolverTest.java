package com.itssum.global.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class PublicPathBearerTokenResolverTest {

    private final PublicPathBearerTokenResolver resolver =
            new PublicPathBearerTokenResolver("/api/auth/tokens/refresh");

    @Test
    void ignoresAuthorizationHeaderOnPublicPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/tokens/refresh");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired.access.token");

        assertNull(resolver.resolve(request));
    }

    @Test
    void readsBearerTokenOnOtherPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat-rooms");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi");

        assertEquals("abc.def.ghi", resolver.resolve(request));
    }
}
