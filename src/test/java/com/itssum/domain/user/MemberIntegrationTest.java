package com.itssum.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.auth.service.EmailVerificationStore;
import com.itssum.domain.user.dto.request.UpdateDonorProfileRequest;
import com.itssum.domain.user.service.DonorProfileService;
import com.itssum.domain.user.service.WithdrawalService;
import com.itssum.global.infra.kakao.KakaoLocalClient;
import com.itssum.global.infra.nts.NtsBusinessClient;
import com.itssum.global.infra.nts.NtsValidation;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 가입 → 내 정보 수정 → 탈퇴 → 같은 이메일·사업자번호로 다시 가입하는 흐름을 실제 DB 로 확인한다.
 * 이메일 인증 흐름은 AuthFlowIntegrationTest 가 확인하므로 여기서는 인증 토큰을 저장소에 직접 넣는다
 * (재발송 간격 60초 때문에 같은 이메일로 인증번호를 바로 다시 받을 수 없다).
 * 커밋 후 처리를 보려고 테스트 트랜잭션을 쓰지 않고, 만든 행은 회원 ID 로 직접 지운다 (탈퇴하면 이메일이 바뀐다).
 */
@SpringBootTest
@AutoConfigureMockMvc
class MemberIntegrationTest {

    private static final LocalDate OPENING_DATE = LocalDate.of(2015, 3, 2);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailVerificationStore emailVerificationStore;

    @Autowired
    private DonorProfileService donorProfileService;

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private NtsBusinessClient ntsBusinessClient;

    @MockitoBean
    private KakaoLocalClient kakaoLocalClient;

    private final String email = "member-flow-" + UUID.randomUUID() + "@example.com";
    private final String businessNumber =
            String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from organization_verifications where business_number = ?", businessNumber);
        for (Long userId : userIds) {
            jdbcTemplate.update("delete from organization_verifications where organization_id = ?", userId);
            jdbcTemplate.update("delete from organizations where user_id = ?", userId);
            jdbcTemplate.update("delete from donors where user_id = ?", userId);
            jdbcTemplate.update("delete from refresh_tokens where user_id = ?", userId);
            jdbcTemplate.update("delete from terms_agreements where user_id = ?", userId);
            jdbcTemplate.update("delete from users where user_id = ?", userId);
        }
    }

    @Test
    void donorProfileUpdateWithdrawAndRejoinWithSameEmail() throws Exception {
        ResultActions signUp = signUpDonor().andExpect(status().isCreated());
        String accessToken = read(signUp, "$.data.accessToken");
        Long userId = userId(signUp);

        // 내 정보 수정 → 조회에 반영
        perform(patch("/api/donors/me"), accessToken, "{\"name\": \"최윤아\", \"phone\": \"01099998888\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_200_1"));
        perform(get("/api/donors/me"), accessToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("최윤아"))
                .andExpect(jsonPath("$.data.phone").value("010-9999-8888"))
                .andExpect(jsonPath("$.data.regionName").value("경기 부천시"));

        // 탈퇴 → 남은 액세스 토큰은 회원 API 에서 401
        perform(delete("/api/users/me"), accessToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_200_5"));
        perform(get("/api/donors/me"), accessToken, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401"));

        // 개인정보는 지워지고 모든 기기에서 로그아웃된다
        Map<String, Object> user = jdbcTemplate.queryForMap(
                "select email, status from users where user_id = ?", userId);
        assertEquals("withdrawn-" + userId + "@itssum.invalid", user.get("email"));
        assertEquals("WITHDRAWN", user.get("status"));
        assertEquals("탈퇴한 회원", jdbcTemplate.queryForObject(
                "select name from donors where user_id = ?", String.class, userId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "select count(*) from refresh_tokens where user_id = ?", Integer.class, userId));

        // 같은 이메일로 다시 가입된다 (새 회원)
        ResultActions rejoin = signUpDonor().andExpect(status().isCreated());
        assertNotEquals(userId, userId(rejoin));
    }

    @Test
    void organizationWithdrawAndVerifySameBusinessNumberAgain() throws Exception {
        when(ntsBusinessClient.validate(eq(businessNumber), eq(OPENING_DATE), anyString()))
                .thenReturn(new NtsValidation("01", "01", "부가가치세 면세사업자", null));
        when(kakaoLocalClient.geocode(any())).thenReturn(Optional.empty());

        String businessToken = read(verifyBusiness().andExpect(status().isOk()), "$.data.businessVerificationToken");
        String emailToken = verifiedEmailToken();
        ResultActions signUp = mockMvc.perform(post("/api/auth/organizations/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailVerificationToken": "%s", "businessVerificationToken": "%s",
                                 "email": "%s", "password": "abcd1234", "name": "구로지역아동센터",
                                 "representativeName": "김수현", "businessNumber": "%s", "openingDate": "2015-03-02",
                                 "phone": "0212345678", "address": "서울 구로구 가마산로 12", "addressDetail": "2층",
                                 "agreedTerms": ["SERVICE", "PRIVACY", "ORG_ELIGIBILITY"]}
                                """.formatted(emailToken, businessToken, email, businessNumber)))
                .andExpect(status().isCreated());
        String accessToken = read(signUp, "$.data.accessToken");
        Long userId = userId(signUp);

        perform(get("/api/organizations/" + userId), accessToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("구로지역아동센터"))
                .andExpect(jsonPath("$.data.addressDetail").value("2층"));

        perform(delete("/api/users/me"), accessToken, null)
                .andExpect(status().isOk());

        // 사업자번호는 비워지고(문자가 섞인 자리표시 값) 기관 정보는 지워진다
        Map<String, Object> organization = jdbcTemplate.queryForMap(
                "select business_number, name, latitude from organizations where user_id = ?", userId);
        assertEquals(String.format("W%09d", userId), organization.get("business_number"));
        assertEquals("탈퇴한 기관", organization.get("name"));
        assertNull(organization.get("latitude"));

        // 같은 사업자번호로 다시 사업자 확인을 받을 수 있다 (409 아님)
        verifyBusiness()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_5"));
    }

    @Test
    void withdrawalWaitsForProfileUpdateInProgressAndKeepsAnonymization() throws Exception {
        Long userId = userId(signUpDonor().andExpect(status().isCreated()));
        CountDownLatch updated = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // B: 수정 트랜잭션이 회원 행을 읽은 뒤에 같은 회원이 탈퇴한다
            Future<?> withdrawal = executor.submit(() -> {
                updated.await();
                withdrawalService.withdraw(userId);
                return null;
            });
            // A: 프로필을 수정하고 커밋 전에 잠시 멈춘다. 잠금이 없으면 이 사이에 탈퇴가 먼저 커밋되고,
            // A 가 나중에 커밋하면서 익명화된 이름을 "최윤아"로 덮어쓴다
            transactionTemplate.executeWithoutResult(status -> {
                donorProfileService.update(userId, new UpdateDonorProfileRequest("최윤아", null, null));
                updated.countDown();
                pause(1000);
            });
            withdrawal.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        // 탈퇴가 수정이 끝난 뒤에 실행되어 수정한 이름까지 익명화된다
        assertEquals("탈퇴한 회원", jdbcTemplate.queryForObject(
                "select name from donors where user_id = ?", String.class, userId));
        assertEquals("WITHDRAWN", jdbcTemplate.queryForObject(
                "select status from users where user_id = ?", String.class, userId));
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private ResultActions signUpDonor() throws Exception {
        return mockMvc.perform(post("/api/auth/donors/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"emailVerificationToken": "%s", "email": "%s", "password": "abcd1234",
                         "name": "최윤서", "birthDate": "1999-03-02", "phone": "01012345678",
                         "agreedTerms": ["SERVICE", "PRIVACY"]}
                        """.formatted(verifiedEmailToken(), email)));
    }

    private ResultActions verifyBusiness() throws Exception {
        return mockMvc.perform(post("/api/auth/organizations/business-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"businessNumber": "%s", "openingDate": "2015-03-02", "representativeName": "김수현"}
                        """.formatted(businessNumber)));
    }

    /** 인증번호 확인을 거친 것처럼 이메일 인증 토큰을 저장소에 넣는다. */
    private String verifiedEmailToken() {
        String token = "verified-" + UUID.randomUUID();
        emailVerificationStore.saveVerifiedToken(token, email);
        return token;
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String accessToken, String body)
            throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private Long userId(ResultActions result) throws Exception {
        Long userId = ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.userId"))
                .longValue();
        userIds.add(userId);
        return userId;
    }

    private static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
