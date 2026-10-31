package com.itssum.domain.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.global.infra.kakao.GeoPoint;
import com.itssum.global.infra.kakao.KakaoLocalClient;
import com.itssum.global.infra.mail.EmailSender;
import com.itssum.global.infra.nts.NtsBusinessClient;
import com.itssum.global.infra.nts.NtsValidation;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 사업자 확인 → 이메일 인증 → 기관 가입 → 로그인까지 실제 DB 로 확인한다. 국세청·카카오 API 와 메일 발송만 가짜로 둔다.
 * 커밋 후 처리(토큰 소모)를 보려고 테스트 트랜잭션을 쓰지 않고, 만든 행은 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrganizationSignUpIntegrationTest {

    private static final Pattern CODE = Pattern.compile("\\d{6}");
    private static final LocalDate OPENING_DATE = LocalDate.of(2015, 3, 2);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailSender emailSender;

    @MockitoBean
    private NtsBusinessClient ntsBusinessClient;

    @MockitoBean
    private KakaoLocalClient kakaoLocalClient;

    private final String email = "org-flow-" + UUID.randomUUID() + "@example.com";
    private final String businessNumber =
            String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));

    @AfterEach
    void cleanUp() {
        String userIds = "(select user_id from users where email = ?)";
        jdbcTemplate.update("delete from organization_verifications where business_number = ?", businessNumber);
        jdbcTemplate.update("delete from organizations where user_id in " + userIds, email);
        jdbcTemplate.update("delete from refresh_tokens where user_id in " + userIds, email);
        jdbcTemplate.update("delete from terms_agreements where user_id in " + userIds, email);
        jdbcTemplate.update("delete from users where email = ?", email);
    }

    @Test
    void verifyBusinessThenSignUpOrganizationAndLogin() throws Exception {
        when(ntsBusinessClient.validate(businessNumber, OPENING_DATE, "김수현"))
                .thenReturn(new NtsValidation("01", "01", "부가가치세 면세사업자", null));
        when(kakaoLocalClient.geocode("서울 구로구 가마산로 12"))
                .thenReturn(Optional.of(new GeoPoint(new BigDecimal("37.4930158"), new BigDecimal("126.8868294"))));
        String hyphenated = businessNumber.substring(0, 3) + "-" + businessNumber.substring(3, 5) + "-"
                + businessNumber.substring(5);

        // 1. 사업자 확인 → 사업자 확인 토큰
        String businessToken = read(perform("/api/auth/organizations/business-verifications", """
                {"businessNumber": "%s", "openingDate": "2015-03-02", "representativeName": "김수현"}
                """.formatted(hyphenated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_200_5")), "$.data.businessVerificationToken");

        // 2. 이메일 인증 → 이메일 인증 토큰
        perform("/api/auth/email-verifications", "{\"email\": \"%s\"}".formatted(email))
                .andExpect(status().isOk());
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(email), anyString(), text.capture());
        Matcher code = CODE.matcher(text.getValue());
        code.find();
        String emailToken = read(perform("/api/auth/email-verifications/confirm",
                "{\"email\": \"%s\", \"code\": \"%s\"}".formatted(email, code.group()))
                .andExpect(status().isOk()), "$.data.emailVerificationToken");

        // 3. 가입 → 바로 로그인 토큰
        perform("/api/auth/organizations/sign-up", """
                {"emailVerificationToken": "%s", "businessVerificationToken": "%s",
                 "email": "%s", "password": "abcd1234", "name": "구로지역아동센터",
                 "representativeName": "김수현", "businessNumber": "%s", "openingDate": "2015-03-02",
                 "phone": "0212345678", "address": "서울 구로구 가마산로 12", "addressDetail": "2층",
                 "agreedTerms": ["SERVICE", "PRIVACY", "ORG_ELIGIBILITY"]}
                """.formatted(emailToken, businessToken, email, businessNumber))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AUTH_201"))
                .andExpect(jsonPath("$.data.role").value("ORGANIZATION"));

        // 4. 기관·약관이 저장되고 사업자 확인 이력이 기관에 연결된다
        Map<String, Object> organization = jdbcTemplate.queryForMap("""
                select o.user_id, o.business_number, o.phone, o.address, o.address_detail, o.latitude, o.longitude,
                       o.verification_status
                from organizations o join users u on u.user_id = o.user_id where u.email = ?
                """, email);
        assertEquals(businessNumber, organization.get("business_number"));
        assertEquals("02-1234-5678", organization.get("phone"));
        assertEquals("서울 구로구 가마산로 12", organization.get("address"));
        assertEquals("2층", organization.get("address_detail"));
        assertEquals(new BigDecimal("37.4930158"), organization.get("latitude"));
        assertEquals(new BigDecimal("126.8868294"), organization.get("longitude"));
        assertEquals("VERIFIED", organization.get("verification_status"));
        assertEquals(organization.get("user_id"), jdbcTemplate.queryForObject(
                "select organization_id from organization_verifications where business_number = ? and result = 'SUCCESS'",
                Long.class, businessNumber));
        assertEquals(List.of("ORG_ELIGIBILITY", "PRIVACY", "SERVICE"), jdbcTemplate.queryForList(
                "select terms_type from terms_agreements where user_id = ? order by terms_type",
                String.class, organization.get("user_id")));

        // 5. 가입된 사업자번호는 다시 확인받을 수 없다
        perform("/api/auth/organizations/business-verifications", """
                {"businessNumber": "%s", "openingDate": "2015-03-02", "representativeName": "김수현"}
                """.formatted(businessNumber))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_409_1"));

        // 6. 기관 계정으로 로그인된다
        perform("/api/auth/login", "{\"email\": \"%s\", \"password\": \"abcd1234\"}".formatted(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ORGANIZATION"));
    }

    private ResultActions perform(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
