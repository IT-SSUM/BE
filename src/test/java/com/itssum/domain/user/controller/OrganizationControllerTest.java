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

import com.itssum.domain.user.dto.request.UpdateOrganizationProfileRequest;
import com.itssum.domain.user.dto.response.OrganizationProfileResponse;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.entity.VerificationStatus;
import com.itssum.domain.user.service.OrganizationProfileService;
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

@WebMvcTest(OrganizationController.class)
@SecurityTestConfig
class OrganizationControllerTest {

    private static final OrganizationProfileResponse PROFILE = new OrganizationProfileResponse("org@example.com",
            "구로지역아동센터", "김수현", "1234567890", LocalDate.of(2015, 3, 2), "02-1234-5678",
            "서울 구로구 가마산로 12", "2층", VerificationStatus.VERIFIED);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @MockitoBean
    private OrganizationProfileService organizationProfileService;

    @Test
    void getMeReturnsProfile() throws Exception {
        when(organizationProfileService.getMine(2L)).thenReturn(PROFILE);

        mockMvc.perform(get("/api/organizations/me").header(HttpHeaders.AUTHORIZATION, bearer(2L, UserRole.ORGANIZATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_200_2"))
                .andExpect(jsonPath("$.data.businessNumber").value("1234567890"))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));
    }

    @Test
    void patchMeReturnsUpdatedProfile() throws Exception {
        when(organizationProfileService.updateMine(eq(2L), any())).thenReturn(PROFILE);

        mockMvc.perform(patch("/api/organizations/me").header(HttpHeaders.AUTHORIZATION, bearer(2L, UserRole.ORGANIZATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "02-1234-5678"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_200_3"));

        verify(organizationProfileService).updateMine(2L,
                new UpdateOrganizationProfileRequest(null, "02-1234-5678", null, null));
    }

    @Test
    void patchMeRejectsBlankAddress() throws Exception {
        mockMvc.perform(patch("/api/organizations/me").header(HttpHeaders.AUTHORIZATION, bearer(2L, UserRole.ORGANIZATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"address": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("address")));

        verifyNoInteractions(organizationProfileService);
    }

    @Test
    void donorTokenIsForbiddenForMe() throws Exception {
        mockMvc.perform(get("/api/organizations/me").header(HttpHeaders.AUTHORIZATION, bearer(1L, UserRole.DONOR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));

        verifyNoInteractions(organizationProfileService);
    }

    private String bearer(Long userId, UserRole role) {
        return "Bearer " + accessTokenProvider.issue(userId, role);
    }
}
