package com.itssum.domain.item.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.item.image.ItemImageKind;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.infra.s3.PresignedImageUpload;
import com.itssum.global.security.AccessTokenProvider;
import com.itssum.support.SecurityTestConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ItemImageController.class)
@SecurityTestConfig
class ItemImageControllerTest {

    private static final PresignedImageUpload UPLOAD = new PresignedImageUpload(
            "donation-items/1/11111111-1111-1111-1111-111111111111.jpg", "https://example.com/upload",
            Map.of("Content-Type", "image/jpeg", "If-None-Match", "*"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @MockitoBean
    private ItemImages itemImages;

    @MockitoBean
    private MemberReader memberReader;

    @Test
    void donorGetsDonationUploadUrl() throws Exception {
        when(itemImages.presignUpload(ItemImageKind.DONATION, 1L, "image/jpeg")).thenReturn(UPLOAD);

        presign("/api/donation-items/images/presign", 1L, UserRole.DONOR, "image/jpeg")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_3"))
                .andExpect(jsonPath("$.data.objectKey").value(UPLOAD.objectKey()))
                .andExpect(jsonPath("$.data.requiredHeaders['If-None-Match']").value("*"));
        verify(memberReader).activeUser(1L);
    }

    @Test
    void organizationCannotGetDonationUploadUrl() throws Exception {
        presign("/api/donation-items/images/presign", 2L, UserRole.ORGANIZATION, "image/jpeg")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        verifyNoInteractions(itemImages);
    }

    @Test
    void organizationGetsNeededUploadUrl() throws Exception {
        when(itemImages.presignUpload(ItemImageKind.NEEDED, 2L, "image/png")).thenReturn(UPLOAD);

        presign("/api/needed-items/images/presign", 2L, UserRole.ORGANIZATION, "image/png")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_3"));
        verify(memberReader).activeUser(2L);
    }

    @Test
    void donorCannotGetNeededUploadUrl() throws Exception {
        presign("/api/needed-items/images/presign", 1L, UserRole.DONOR, "image/png")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));
        verifyNoInteractions(itemImages);
    }

    @Test
    void blankContentTypeIs400() throws Exception {
        presign("/api/donation-items/images/presign", 1L, UserRole.DONOR, " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
        verifyNoInteractions(itemImages);
    }

    private ResultActions presign(String path, Long userId, UserRole role, String contentType) throws Exception {
        return mockMvc.perform(post(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenProvider.issue(userId, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentType\": \"" + contentType + "\"}"));
    }
}
