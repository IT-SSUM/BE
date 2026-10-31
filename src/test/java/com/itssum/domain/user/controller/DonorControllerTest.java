package com.itssum.domain.user.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.user.dto.request.UpdateDonorProfileRequest;
import com.itssum.domain.user.dto.response.DonorProfileResponse;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.service.DonorProfileService;
import com.itssum.global.security.AccessTokenProvider;
import com.itssum.support.SecurityTestConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DonorController.class)
@SecurityTestConfig
class DonorControllerTest {

    private static final DonorProfileResponse PROFILE = new DonorProfileResponse("donor@example.com", "최윤서",
            LocalDate.of(1999, 3, 2), "010-1234-5678", 1L, "경기 부천시");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @MockitoBean
    private DonorProfileService donorProfileService;

    @Test
    void getMeReturnsProfile() throws Exception {
        when(donorProfileService.get(1L)).thenReturn(PROFILE);

        mockMvc.perform(get("/api/donors/me").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.DONOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_200"))
                .andExpect(jsonPath("$.data.email").value("donor@example.com"))
                .andExpect(jsonPath("$.data.regionName").value("경기 부천시"));
    }

    @Test
    void patchMeReturnsUpdatedProfile() throws Exception {
        when(donorProfileService.update(eq(1L), any())).thenReturn(PROFILE);

        mockMvc.perform(patch("/api/donors/me").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.DONOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "010-9999-8888"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_200_1"));

        verify(donorProfileService).update(1L, new UpdateDonorProfileRequest(null, null, "010-9999-8888"));
    }

    @Test
    void patchMeRejectsBlankName() throws Exception {
        mockMvc.perform(patch("/api/donors/me").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.DONOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));

        verifyNoInteractions(donorProfileService);
    }

    @Test
    void organizationTokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/donors/me").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.ORGANIZATION)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));

        verifyNoInteractions(donorProfileService);
    }

    private String bearer(UserRole role) {
        return "Bearer " + accessTokenProvider.issue(1L, role);
    }
}
