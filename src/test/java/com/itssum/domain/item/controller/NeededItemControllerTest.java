package com.itssum.domain.item.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.item.dto.response.NeededItemDetailResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.NeededItemStatus;
import com.itssum.domain.item.service.NeededItemCommandService;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.entity.VerificationStatus;
import com.itssum.global.security.AccessTokenProvider;
import com.itssum.support.SecurityTestConfig;
import java.time.LocalDateTime;
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
