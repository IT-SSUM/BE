package com.itssum.domain.user.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.service.WithdrawalService;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.security.AccessTokenProvider;
import com.itssum.support.SecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@SecurityTestConfig
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @MockitoBean
    private WithdrawalService withdrawalService;

    @Test
    void withdrawReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_200_5"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(withdrawalService).withdraw(1L);
    }

    @Test
    void blockedWithdrawalIs409() throws Exception {
        doThrow(new BusinessException(ErrorCode.USER_WITHDRAWAL_BLOCKED)).when(withdrawalService).withdraw(1L);

        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_409"))
                .andExpect(jsonPath("$.message").value(
                        "진행 중인 거래가 있어 탈퇴할 수 없습니다. 응답 대기 중인 제안·요청과 거래중·예약중인 거래를 먼저 마무리해 주세요."));
    }

    @Test
    void withoutTokenIs401() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401"));

        verifyNoInteractions(withdrawalService);
    }

    private String bearer() {
        return "Bearer " + accessTokenProvider.issue(1L, UserRole.DONOR);
    }
}
