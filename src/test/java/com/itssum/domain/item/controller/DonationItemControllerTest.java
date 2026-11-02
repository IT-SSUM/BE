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

import com.itssum.domain.item.dto.request.UpdateDonationItemRequest;
import com.itssum.domain.item.dto.response.DonationItemDetailResponse;
import com.itssum.domain.item.dto.response.ItemPageResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItemStatus;
import com.itssum.domain.item.service.DonationItemCommandService;
import com.itssum.domain.item.service.DonationItemQueryService;
import com.itssum.domain.item.service.ItemSearchCondition;
import com.itssum.domain.user.entity.UserRole;
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

@WebMvcTest(DonationItemController.class)
@SecurityTestConfig
class DonationItemControllerTest {

    static final DonationItemDetailResponse DETAIL = new DonationItemDetailResponse(10L, DonationItemStatus.RECRUITING,
            1L, "생활용품", "수건", 30, 0, 0, 30, "장", ConditionGrade.MID, "깨끗한 수건이에요", "경기 부천시",
            List.of("https://example.com/a"), LocalDateTime.of(2026, 11, 2, 10, 0), true, 1L, "최윤서",
            "이웃에게 물품을 전해요", "경기 부천시 길주로 210", "2층", List.of("donation-items/1/a.jpg"));

    private static final String CREATE_BODY = """
            {"categoryId": 1, "itemName": "수건", "quantity": 30, "unit": "장", "conditionGrade": "MID",
             "imageKeys": ["donation-items/1/a.jpg"], "pickupAddress": "경기 부천시 길주로 210", "pickupAddressDetail": "2층"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @MockitoBean
    private DonationItemCommandService commandService;

    @MockitoBean
    private DonationItemQueryService queryService;

    @Test
    void organizationBrowses200() throws Exception {
        when(queryService.browse(eq(2L), any(), eq(30L), eq(10))).thenReturn(new ItemPageResponse<>(List.of(), null, false));

        perform(get("/api/donation-items").param("keyword", "수건").param("categoryId", "1").param("condition", "MID")
                .param("beforeId", "30").param("size", "10"), 2L, UserRole.ORGANIZATION, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_4"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
        verify(queryService).browse(2L, new ItemSearchCondition("수건", 1L, null, ConditionGrade.MID), 30L, 10);
    }

    @Test
    void donorBrowsesToo200() throws Exception {
        when(queryService.browse(eq(1L), any(), isNull(), eq(20))).thenReturn(new ItemPageResponse<>(List.of(), null, false));

        perform(get("/api/donation-items"), 1L, UserRole.DONOR, null)
                .andExpect(status().isOk());
    }

    @Test
    void anyMemberSeesDetail() throws Exception {
        when(queryService.detail(2L, 10L)).thenReturn(DETAIL);

        perform(get("/api/donation-items/10"), 2L, UserRole.ORGANIZATION, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_5"))
                .andExpect(jsonPath("$.data.itemName").value("수건"));
    }

    @Test
    void donorListsMine() throws Exception {
        when(queryService.mine(1L, 5L, null, 20)).thenReturn(new ItemPageResponse<>(List.of(), null, false));

        perform(get("/api/donors/me/donation-items").param("categoryId", "5"), 1L, UserRole.DONOR, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_8"));
    }

    @Test
    void donorPatches200() throws Exception {
        when(commandService.update(eq(1L), eq(10L), any())).thenReturn(DETAIL);

        perform(patch("/api/donation-items/10"), 1L, UserRole.DONOR, "{\"description\": \"새 설명\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_6"));
        verify(commandService).update(1L, 10L, new UpdateDonationItemRequest("새 설명", null, null, null));
    }

    @Test
    void patchBlankPickupAddressIs400() throws Exception {
        perform(patch("/api/donation-items/10"), 1L, UserRole.DONOR, "{\"pickupAddress\": \"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(commandService);
    }

    @Test
    void patchEmptyImageListIs400() throws Exception {
        perform(patch("/api/donation-items/10"), 1L, UserRole.DONOR, "{\"imageKeys\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(commandService);
    }

    @Test
    void organizationCannotPatchOrDelete403() throws Exception {
        perform(patch("/api/donation-items/10"), 2L, UserRole.ORGANIZATION, "{\"description\": \"x\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        perform(delete("/api/donation-items/10"), 2L, UserRole.ORGANIZATION, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        verifyNoInteractions(commandService);
    }

    @Test
    void donorDeletes200() throws Exception {
        perform(delete("/api/donation-items/10"), 1L, UserRole.DONOR, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_7"));
        verify(commandService).delete(1L, 10L);
    }

    @Test
    void organizationCannotListDonorMine403() throws Exception {
        perform(get("/api/donors/me/donation-items"), 2L, UserRole.ORGANIZATION, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        verifyNoInteractions(queryService);
    }

    @Test
    void donorCreates201() throws Exception {
        when(commandService.create(eq(1L), any())).thenReturn(DETAIL);

        perform(post("/api/donation-items"), 1L, UserRole.DONOR, CREATE_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ITEM_201"))
                .andExpect(jsonPath("$.data.donationItemId").value(10))
                .andExpect(jsonPath("$.data.regionName").value("경기 부천시"));
    }

    @Test
    void organizationCannotCreate() throws Exception {
        perform(post("/api/donation-items"), 2L, UserRole.ORGANIZATION, CREATE_BODY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        verifyNoInteractions(commandService);
    }

    @Test
    void missingImagesIs400() throws Exception {
        perform(post("/api/donation-items"), 1L, UserRole.DONOR, CREATE_BODY.replace(
                "\"imageKeys\": [\"donation-items/1/a.jpg\"], ", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        perform(post("/api/donation-items"), 1L, UserRole.DONOR, CREATE_BODY.replace(
                "[\"donation-items/1/a.jpg\"]", "[]"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(commandService);
    }

    @Test
    void sixImagesIs400() throws Exception {
        perform(post("/api/donation-items"), 1L, UserRole.DONOR, CREATE_BODY.replace(
                "[\"donation-items/1/a.jpg\"]", "[\"a\", \"b\", \"c\", \"d\", \"e\", \"f\"]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(commandService);
    }

    @Test
    void zeroQuantityIs400() throws Exception {
        perform(post("/api/donation-items"), 1L, UserRole.DONOR, CREATE_BODY.replace("\"quantity\": 30", "\"quantity\": 0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(commandService);
    }

    @Test
    void fullWidthBlankItemNameIs400() throws Exception {
        perform(post("/api/donation-items"), 1L, UserRole.DONOR, CREATE_BODY.replace("\"수건\"", "\"　\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(commandService);
    }

    @Test
    void missingPickupAddressIs400() throws Exception {
        perform(post("/api/donation-items"), 1L, UserRole.DONOR, CREATE_BODY.replace(
                "\"pickupAddress\": \"경기 부천시 길주로 210\", ", ""))
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
