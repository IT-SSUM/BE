package com.itssum.domain.item.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.item.dto.request.UpdateNeededItemRequest;
import com.itssum.domain.item.dto.response.ItemPageResponse;
import com.itssum.domain.item.dto.response.NeededItemDetailResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.NeededItemStatus;
import com.itssum.domain.item.service.ItemSearchCondition;
import com.itssum.domain.item.service.NeededItemCommandService;
import com.itssum.domain.item.service.NeededItemQueryService;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.entity.VerificationStatus;
import com.itssum.global.security.AccessTokenProvider;
import com.itssum.support.SecurityTestConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(NeededItemController.class)
@SecurityTestConfig
class NeededItemControllerTest {

    static final NeededItemDetailResponse DETAIL = new NeededItemDetailResponse(20L, NeededItemStatus.RECRUITING, 1L,
            "생활용품", "생수", 20, 0, 0, 20, "병", ConditionGrade.HIGH, "아이들이 마실 생수가 필요해요", "경기 부천시",
            "https://example.com/ref", LocalDateTime.of(2026, 11, 2, 10, 0), true, 2L, "구로지역아동센터",
            VerificationStatus.VERIFIED, "서울 구로구 가마산로 12", "아이들의 일상을 함께해요", null, "needed-items/2/ref.jpg");

    private static final String CREATE_BODY = """
            {"categoryId": 1, "itemName": "생수", "requiredQuantity": 20, "unit": "병", "desiredCondition": "HIGH",
             "description": "아이들이 마실 생수가 필요해요"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @MockitoBean
    private NeededItemCommandService commandService;

    @MockitoBean
    private NeededItemQueryService queryService;

    @Test
    void donorBrowses200() throws Exception {
        when(queryService.browse(eq(1L), any(), isNull(), eq(20))).thenReturn(new ItemPageResponse<>(List.of(), null, false));

        perform(get("/api/needed-items").param("keyword", "생수").param("condition", "HIGH"), 1L, UserRole.DONOR, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_9"));
        verify(queryService).browse(1L, new ItemSearchCondition("생수", null, null, ConditionGrade.HIGH), null, 20);
    }

    @Test
    void anyMemberSeesDetail() throws Exception {
        when(queryService.detail(1L, 20L)).thenReturn(DETAIL);

        perform(get("/api/needed-items/20"), 1L, UserRole.DONOR, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_10"))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));
    }

    @Test
    void organizationListsMine() throws Exception {
        when(queryService.mine(2L, null, null, 20)).thenReturn(new ItemPageResponse<>(List.of(), null, false));

        perform(get("/api/organizations/me/needed-items"), 2L, UserRole.ORGANIZATION, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_13"));
    }

    @Test
    void organizationPatches200() throws Exception {
        when(commandService.update(eq(2L), eq(20L), any())).thenReturn(DETAIL);

        perform(patch("/api/needed-items/20"), 2L, UserRole.ORGANIZATION, "{\"receiveAddress\": \"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_11"));
        verify(commandService).update(2L, 20L, new UpdateNeededItemRequest(null, "", null));
    }

    @Test
    void donorCannotPatchOrDelete403() throws Exception {
        perform(patch("/api/needed-items/20"), 1L, UserRole.DONOR, "{\"description\": \"x\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        perform(delete("/api/needed-items/20"), 1L, UserRole.DONOR, null)
                .andExpect(status().isForbidden());
        verifyNoInteractions(commandService);
    }

    @Test
    void organizationDeletes200() throws Exception {
        perform(delete("/api/needed-items/20"), 2L, UserRole.ORGANIZATION, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_12"));
        verify(commandService).delete(2L, 20L);
    }

    @Test
    void donorCannotListOrganizationMine403() throws Exception {
        perform(get("/api/organizations/me/needed-items"), 1L, UserRole.DONOR, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        verifyNoInteractions(queryService);
    }

    @Test
    void organizationCreates201() throws Exception {
        when(commandService.create(eq(2L), any())).thenReturn(DETAIL);

        perform(post("/api/needed-items"), 2L, UserRole.ORGANIZATION, CREATE_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ITEM_201_1"))
                .andExpect(jsonPath("$.data.neededItemId").value(20))
                .andExpect(jsonPath("$.data.organizationName").value("구로지역아동센터"));
    }

    @Test
    void donorCannotCreate403() throws Exception {
        perform(post("/api/needed-items"), 1L, UserRole.DONOR, CREATE_BODY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        verifyNoInteractions(commandService);
    }

    @Test
    void zeroQuantityIs400() throws Exception {
        perform(post("/api/needed-items"), 2L, UserRole.ORGANIZATION,
                CREATE_BODY.replace("\"requiredQuantity\": 20", "\"requiredQuantity\": 0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(commandService);
    }

    @Test
    void missingDesiredConditionIs400() throws Exception {
        perform(post("/api/needed-items"), 2L, UserRole.ORGANIZATION,
                CREATE_BODY.replace("\"desiredCondition\": \"HIGH\",", ""))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(commandService);
    }

    ResultActions perform(MockHttpServletRequestBuilder request, Long userId, UserRole role, String body)
            throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenProvider.issue(userId, role));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }
}
