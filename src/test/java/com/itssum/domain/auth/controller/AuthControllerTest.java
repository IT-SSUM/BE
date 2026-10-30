package com.itssum.domain.auth.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.auth.dto.request.LogoutRequest;
import com.itssum.domain.auth.dto.response.AuthTokenResponse;
import com.itssum.domain.auth.dto.response.EmailVerificationConfirmResponse;
import com.itssum.domain.auth.dto.response.EmailVerificationSendResponse;
import com.itssum.domain.auth.service.AuthService;
import com.itssum.domain.auth.service.EmailVerificationService;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.support.SecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AuthController.class)
@SecurityTestConfig
class AuthControllerTest {

    private static final AuthTokenResponse TOKENS = new AuthTokenResponse(1L, UserRole.DONOR, "access", "refresh");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @Test
    void sendEmailVerificationWithoutToken() throws Exception {
        when(emailVerificationService.send(any())).thenReturn(new EmailVerificationSendResponse(300));

        mockMvc.perform(json("/api/auth/email-verifications", """
                        {"email": "donor@example.com"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("AUTH_200"))
                .andExpect(jsonPath("$.message").value("인증번호를 보냈습니다."))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300));
    }

    @Test
    void confirmEmailVerificationReturnsToken() throws Exception {
        when(emailVerificationService.confirm(any())).thenReturn(new EmailVerificationConfirmResponse("verify-token"));

        mockMvc.perform(json("/api/auth/email-verifications/confirm", """
                        {"email": "donor@example.com", "code": "123456"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_1"))
                .andExpect(jsonPath("$.data.emailVerificationToken").value("verify-token"));
    }

    @Test
    void signUpDonorReturnsTokens() throws Exception {
        when(authService.signUpDonor(any())).thenReturn(TOKENS);

        mockMvc.perform(json("/api/auth/donors/sign-up", """
                        {"emailVerificationToken": "verify-token", "email": "donor@example.com",
                         "password": "abcd1234", "name": "최윤서", "birthDate": "1999-03-02",
                         "phone": "010-1234-5678", "regionId": 12, "agreedTerms": ["SERVICE", "PRIVACY"]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AUTH_201"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.role").value("DONOR"))
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"));
    }

    @Test
    void signUpValidationFailureReturnsFieldErrors() throws Exception {
        mockMvc.perform(json("/api/auth/donors/sign-up", """
                        {"emailVerificationToken": "verify-token", "email": "donor@example.com",
                         "password": "onlyletters", "name": "최윤서",
                         "phone": "1234", "agreedTerms": ["SERVICE", "PRIVACY"]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("password", "phone", "birthDate")));
    }

    @Test
    void loginReturnsTokens() throws Exception {
        when(authService.login(any())).thenReturn(TOKENS);

        mockMvc.perform(json("/api/auth/login", """
                        {"email": "donor@example.com", "password": "abcd1234"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_2"))
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }

    @Test
    void loginFailureUsesAuthErrorCode() throws Exception {
        when(authService.login(any())).thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        mockMvc.perform(json("/api/auth/login", """
                        {"email": "donor@example.com", "password": "wrong"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_401"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void refreshIgnoresExpiredBearerHeader() throws Exception {
        when(authService.refresh(any())).thenReturn(TOKENS);

        mockMvc.perform(json("/api/auth/tokens/refresh", """
                        {"refreshToken": "old-refresh"}
                        """).header(HttpHeaders.AUTHORIZATION, "Bearer expired.access.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_3"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"));
    }

    @Test
    void logoutWithoutAccessToken() throws Exception {
        mockMvc.perform(json("/api/auth/logout", """
                        {"refreshToken": "old-refresh"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("AUTH_200_4"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(authService).logout(new LogoutRequest("old-refresh"));
    }

    private static MockHttpServletRequestBuilder json(String path, String body) {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
