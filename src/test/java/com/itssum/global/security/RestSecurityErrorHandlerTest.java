package com.itssum.global.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.json.JsonMapper;

class RestSecurityErrorHandlerTest {

    private final RestSecurityErrorHandler handler = new RestSecurityErrorHandler(JsonMapper.builder().build());

    @Test
    void authenticationFailureIsWrittenAsCommon401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(new MockHttpServletRequest(), response, new InsufficientAuthenticationException("none"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("\"code\":\"COMMON_401\""));
    }

    @Test
    void accessDeniedIsWrittenAsCommon403() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"COMMON_403\""));
    }
}
