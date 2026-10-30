package com.itssum.domain.auth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.global.infra.mail.EmailSender;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 인증번호 발송 → 확인 → 가입 → 보호 API → 재발급(교체) → 로그인 → 로그아웃까지 실제 DB 로 확인한다.
 * 커밋 후 처리(인증 토큰 소모)를 보려고 테스트 트랜잭션을 쓰지 않고, 만든 행은 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    private static final Pattern CODE = Pattern.compile("\\d{6}");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailSender emailSender;

    private final String email = "flow-" + UUID.randomUUID() + "@example.com";

    @AfterEach
    void cleanUp() {
        String userIds = "(select user_id from users where email = ?)";
        jdbcTemplate.update("delete from refresh_tokens where user_id in " + userIds, email);
        jdbcTemplate.update("delete from terms_agreements where user_id in " + userIds, email);
        jdbcTemplate.update("delete from donors where user_id in " + userIds, email);
        jdbcTemplate.update("delete from users where email = ?", email);
    }

    @Test
    void signUpRefreshLoginAndLogout() throws Exception {
        // 1. 인증번호 발송 → 가짜 발송기에서 번호를 꺼낸다
        perform("/api/auth/email-verifications", "{\"email\": \"%s\"}".formatted(email))
                .andExpect(status().isOk());
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(email), anyString(), text.capture());
        Matcher code = CODE.matcher(text.getValue());
        code.find();

        // 2. 확인 → 인증 토큰
        String verificationToken = read(perform("/api/auth/email-verifications/confirm",
                "{\"email\": \"%s\", \"code\": \"%s\"}".formatted(email, code.group()))
                .andExpect(status().isOk()), "$.data.emailVerificationToken");

        // 3. 가입 → 바로 로그인 토큰
        String signUpBody = """
                {"emailVerificationToken": "%s", "email": "%s", "password": "abcd1234",
                 "name": "최윤서", "birthDate": "1999-03-02", "phone": "01012345678",
                 "agreedTerms": ["SERVICE", "PRIVACY"]}
                """.formatted(verificationToken, email);
        ResultActions signUp = perform("/api/auth/donors/sign-up", signUpBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("DONOR"));
        String accessToken = read(signUp, "$.data.accessToken");
        String firstRefresh = read(signUp, "$.data.refreshToken");

        // 4. 같은 인증 토큰은 다시 쓸 수 없다 (커밋 후 소모)
        perform("/api/auth/donors/sign-up", signUpBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_400_3"));

        // 5. 액세스 토큰으로 보호된 API 호출
        mockMvc.perform(get("/api/chat-rooms").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 6. 재발급 → 새 토큰, 쓴 토큰은 401
        String secondRefresh = read(perform("/api/auth/tokens/refresh",
                "{\"refreshToken\": \"%s\"}".formatted(firstRefresh))
                .andExpect(status().isOk()), "$.data.refreshToken");
        perform("/api/auth/tokens/refresh", "{\"refreshToken\": \"%s\"}".formatted(firstRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_401_1"));

        // 7. 대소문자가 달라도 같은 이메일로 로그인된다
        perform("/api/auth/login", "{\"email\": \"%s\", \"password\": \"abcd1234\"}".formatted(email.toUpperCase()))
                .andExpect(status().isOk());

        // 8. 로그아웃하면 그 기기의 리프레시 토큰은 더 쓸 수 없다
        perform("/api/auth/logout", "{\"refreshToken\": \"%s\"}".formatted(secondRefresh))
                .andExpect(status().isOk());
        perform("/api/auth/tokens/refresh", "{\"refreshToken\": \"%s\"}".formatted(secondRefresh))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions perform(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
