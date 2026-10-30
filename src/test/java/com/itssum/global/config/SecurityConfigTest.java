package com.itssum.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.user.entity.UserRole;
import com.itssum.global.security.AccessTokenProvider;
import com.itssum.support.SecurityTestConfig;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.ProbeController.class)
@Import(SecurityConfigTest.ProbeController.class)
@SecurityTestConfig
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @Test
    void protectedApiWithoutTokenReturnsCommon401() throws Exception {
        mockMvc.perform(get("/api/test/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_401"));
    }

    @Test
    void protectedApiWithInvalidTokenReturnsCommon401() throws Exception {
        mockMvc.perform(get("/api/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401"));
    }

    @Test
    void protectedApiWithValidTokenSeesUserIdAsPrincipalName() throws Exception {
        String token = accessTokenProvider.issue(7L, UserRole.DONOR);

        mockMvc.perform(get("/api/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void publicAuthPathsAreOpenOnlyForPost() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void itemConditionsStayPublic() throws Exception {
        // 컨트롤러가 없어 404 지만 인가 단계(401)는 통과해야 한다
        mockMvc.perform(get("/api/item-conditions"))
                .andExpect(status().isNotFound());
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/test/me")
        String me(Principal principal) {
            return principal.getName();
        }
    }
}
