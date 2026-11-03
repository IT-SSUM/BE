package com.itssum.domain.recommendation.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.recommendation.dto.response.OrganizationRecommendationResponse;
import com.itssum.domain.recommendation.service.RecommendationService;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.security.AccessTokenProvider;
import com.itssum.support.SecurityTestConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(RecommendationController.class)
@SecurityTestConfig
class RecommendationControllerTest {

    private static final String PATH = "/api/donation-items/10/recommendations";
    private static final OrganizationRecommendationResponse FIRST = new OrganizationRecommendationResponse(1, 2L,
            "구로지역아동센터", "경기 부천시 길주로 210", 7L, "어린이 도서", 30, "권", ConditionGrade.MID,
            new BigDecimal("96.00"), new BigDecimal("2.00"), null);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    void donorRecommends200() throws Exception {
        when(recommendationService.recommend(1L, 10L)).thenReturn(List.of(FIRST));

        perform(post(PATH), 1L, UserRole.DONOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_200"))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].organizationName").value("구로지역아동센터"))
                .andExpect(jsonPath("$.data[0].neededItemId").value(7));
        verify(recommendationService).recommend(1L, 10L);
    }

    @Test
    void donorFinds200() throws Exception {
        when(recommendationService.find(1L, 10L)).thenReturn(List.of(FIRST));

        perform(get(PATH), 1L, UserRole.DONOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_200_1"))
                .andExpect(jsonPath("$.data[0].score").value(96.0));
        verify(recommendationService).find(1L, 10L);
    }

    @Test
    void organizationCannotRecommend() throws Exception {
        perform(post(PATH), 2L, UserRole.ORGANIZATION)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        verifyNoInteractions(recommendationService);
    }

    @Test
    void organizationCannotFind() throws Exception {
        perform(get(PATH), 2L, UserRole.ORGANIZATION)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        verifyNoInteractions(recommendationService);
    }

    @Test
    void notOwnerIs403() throws Exception {
        when(recommendationService.find(1L, 10L))
                .thenThrow(new BusinessException(ErrorCode.RECOMMENDATION_NOT_OWNER));

        perform(get(PATH), 1L, UserRole.DONOR)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_403"));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, Long userId, UserRole role) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + accessTokenProvider.issue(userId, role)));
    }
}
