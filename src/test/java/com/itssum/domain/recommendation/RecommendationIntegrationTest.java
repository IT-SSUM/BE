package com.itssum.domain.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.item.repository.ItemCategoryRepository;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.item.repository.RegionRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.RequesterType;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.repository.DonorRepository;
import com.itssum.domain.user.repository.OrganizationRepository;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.security.AccessTokenProvider;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 추천 계산(POST) → 재계산 → 기관 휴·폐업·진행 중 거래 뒤 조회(GET) → 권한 → 예약마감까지 실제 DB 로 확인한다.
 * 기획안 계산 예시(A 2km·20개, B 5km·25개, C 1km·30개 → A, C, B)를 좌표로 재현한다. 위도 1° = 111.19493km.
 * 다른 테스트 데이터와 섞이지 않게 카테고리 6(유아·아동용품)과 단위 "켤레"만 쓰고, 만든 행은 회원 ID 로 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecommendationIntegrationTest {

    private static final long CATEGORY_ID = 6L;
    private static final String UNIT = "켤레";
    private static final BigDecimal LONGITUDE = new BigDecimal("126.7000000");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DonationItemRepository donationItemRepository;

    @Autowired
    private NeededItemRepository neededItemRepository;

    @Autowired
    private ItemCategoryRepository itemCategoryRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String token = UUID.randomUUID().toString().substring(0, 8);
    private final List<Long> organizationIds = new ArrayList<>();
    private final List<Long> donorIds = new ArrayList<>();

    private Long donorId;
    private Long otherDonorId;
    private Long donationItemId;
    private Long orgA;
    private Long orgB;
    private Long orgC;
    private Long neededA;

    @BeforeEach
    void setUp() {
        donorId = donor("donor");
        otherDonorId = donor("other");
        donationItemId = transactionTemplate.execute(status -> donationItemRepository.save(DonationItem.builder()
                .donor(donorRepository.getReferenceById(donorId)).category(itemCategoryRepository.getReferenceById(CATEGORY_ID))
                .region(regionRepository.getReferenceById(Region.PILOT_REGION_ID)).pickupAddress("경기 부천시 길주로 210")
                .latitude(new BigDecimal("37.5000000")).longitude(LONGITUDE).itemName("아동 운동화" + token)
                .quantity(20).unit(UNIT).conditionGrade(ConditionGrade.MID).build()).getId());

        orgA = organization("A", "37.5179866");
        orgB = organization("B", "37.5449665");
        orgC = organization("C", "37.5089933");
        neededA = needed(orgA, CATEGORY_ID, UNIT, ConditionGrade.MID, 20);
        needed(orgB, CATEGORY_ID, UNIT, ConditionGrade.MID, 25);
        needed(orgC, CATEGORY_ID, UNIT, ConditionGrade.MID, 30);

        // 기관 X: 조건이 하나씩 어긋난 필요 물품 다섯 개 → X 는 추천되면 안 된다
        Long orgX = organization("X", "37.5000000");
        needed(orgX, CATEGORY_ID, UNIT, ConditionGrade.HIGH, 20);
        needed(orgX, CATEGORY_ID, "개", ConditionGrade.MID, 20);
        needed(orgX, 5L, UNIT, ConditionGrade.MID, 20);
        Long closed = needed(orgX, CATEGORY_ID, UNIT, ConditionGrade.MID, 20);
        jdbcTemplate.update("update needed_items set status = 'CLOSED' where needed_item_id = ?", closed);
        Long deleted = needed(orgX, CATEGORY_ID, UNIT, ConditionGrade.MID, 20);
        jdbcTemplate.update("update needed_items set deleted_at = now() where needed_item_id = ?", deleted);

        // 기관 F: 휴·폐업 감지
        Long orgF = organization("F", "37.5000000");
        needed(orgF, CATEGORY_ID, UNIT, ConditionGrade.MID, 20);
        jdbcTemplate.update("update organizations set verification_status = 'SUSPENDED' where user_id = ?", orgF);

        // 기관 G: 기관이 먼저 보낸 기부 요청이 응답 대기 중
        Long orgG = organization("G", "37.5000000");
        Long neededG = needed(orgG, CATEGORY_ID, UNIT, ConditionGrade.MID, 20);
        pendingMatch(neededG, orgG, RequesterType.ORGANIZATION);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from organization_recommendations where donation_item_id = ?", donationItemId);
        jdbcTemplate.update("delete from notifications where receiver_id in (" + placeholders() + ")", allMemberIds());
        jdbcTemplate.update("delete from matches where donation_item_id = ?", donationItemId);
        jdbcTemplate.update("delete from donation_items where donor_id in (" + placeholders(donorIds) + ")",
                donorIds.toArray());
        jdbcTemplate.update("delete from needed_items where organization_id in (" + placeholders(organizationIds) + ")",
                organizationIds.toArray());
        jdbcTemplate.update("delete from donors where user_id in (" + placeholders(donorIds) + ")", donorIds.toArray());
        jdbcTemplate.update("delete from organizations where user_id in (" + placeholders(organizationIds) + ")",
                organizationIds.toArray());
        jdbcTemplate.update("delete from users where user_id in (" + placeholders() + ")", allMemberIds());
    }

    @Test
    void recommendRecalculateFilterAndClear() throws Exception {
        String path = "/api/donation-items/" + donationItemId + "/recommendations";

        // 1. 계산: 기획안 예시 순서 A, C, B. 조건이 어긋난 X·F·G 는 빠진다
        ResultActions first = perform(post(path), donorId, UserRole.DONOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_200"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].score").value(96.0))
                .andExpect(jsonPath("$.data[1].score").value(78.0))
                .andExpect(jsonPath("$.data[2].score").value(78.0))
                .andExpect(jsonPath("$.data[0].distanceKm").value(2.0))
                .andExpect(jsonPath("$.data[1].distanceKm").value(1.0))
                .andExpect(jsonPath("$.data[2].distanceKm").value(5.0))
                .andExpect(jsonPath("$.data[0].reason").doesNotExist());
        assertEquals(List.of(orgA, orgC, orgB), organizationIds(first));
        assertEquals(3, savedRows());

        // 2. 다시 계산해도 UNIQUE 충돌 없이 행이 바뀐다
        perform(post(path), donorId, UserRole.DONOR).andExpect(status().isOk());
        assertEquals(3, savedRows());

        // 3. C 가 휴·폐업 → 조회에서 빠지고 순위를 다시 매긴다
        jdbcTemplate.update("update organizations set verification_status = 'SUSPENDED' where user_id = ?", orgC);
        ResultActions afterSuspend = perform(get(path), donorId, UserRole.DONOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_200_1"))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[1].rank").value(2));
        assertEquals(List.of(orgA, orgB), organizationIds(afterSuspend));

        // 4. A 에 기부 제안이 응답 대기 중 → 조회에서 빠진다
        pendingMatch(neededA, orgA, RequesterType.DONOR);
        ResultActions afterProposal = perform(get(path), donorId, UserRole.DONOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].rank").value(1));
        assertEquals(List.of(orgB), organizationIds(afterProposal));

        // 5. 남의 물품, 기관은 볼 수 없다
        perform(get(path), otherDonorId, UserRole.DONOR)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_403"));
        perform(get(path), orgA, UserRole.ORGANIZATION)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403"));

        // 6. 예약마감(신청 가능 수량 0) → 빈 목록, 이전 추천도 지운다
        jdbcTemplate.update("update donation_items set reserved_quantity = 20 where donation_item_id = ?",
                donationItemId);
        perform(post(path), donorId, UserRole.DONOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        assertEquals(0, savedRows());
    }

    private Long donor(String label) {
        Long id = transactionTemplate.execute(status -> {
            User user = userRepository.save(User.builder().email("rec-" + label + "-" + token + "@example.com")
                    .password("test").role(UserRole.DONOR).build());
            donorRepository.save(Donor.builder().user(user).name(label + token)
                    .birthDate(LocalDate.of(2000, 1, 1)).phone("010-1234-5678").build());
            return user.getId();
        });
        donorIds.add(id);
        return id;
    }

    private Long organization(String label, String latitude) {
        Long id = transactionTemplate.execute(status -> {
            User user = userRepository.save(User.builder().email("rec-org-" + label + "-" + token + "@example.com")
                    .password("test").role(UserRole.ORGANIZATION).build());
            organizationRepository.save(Organization.builder().user(user).name("센터" + label + token)
                    .representativeName("김수현")
                    .businessNumber(String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L)))
                    .openingDate(LocalDate.of(2015, 3, 2)).phone("032-123-4567").address("경기 부천시 길주로 " + label)
                    .latitude(new BigDecimal(latitude)).longitude(LONGITUDE).verifiedAt(LocalDateTime.now()).build());
            return user.getId();
        });
        organizationIds.add(id);
        return id;
    }

    private Long needed(Long organizationId, Long categoryId, String unit, ConditionGrade desired, int quantity) {
        return transactionTemplate.execute(status -> neededItemRepository.save(NeededItem.builder()
                .organization(organizationRepository.getReferenceById(organizationId))
                .category(itemCategoryRepository.getReferenceById(categoryId))
                .region(regionRepository.getReferenceById(Region.PILOT_REGION_ID)).itemName("아동 운동화")
                .requiredQuantity(quantity).unit(unit).desiredCondition(desired).build()).getId());
    }

    private void pendingMatch(Long neededItemId, Long organizationId, RequesterType requestedBy) {
        transactionTemplate.executeWithoutResult(status -> matchRepository.save(Match.builder()
                .donationItem(donationItemRepository.getReferenceById(donationItemId))
                .neededItem(neededItemRepository.getReferenceById(neededItemId))
                .donor(donorRepository.getReferenceById(donorId))
                .organization(organizationRepository.getReferenceById(organizationId))
                .requestedBy(requestedBy).quantity(5).build()));
    }

    private int savedRows() {
        return jdbcTemplate.queryForObject("select count(*) from organization_recommendations where donation_item_id = ?",
                Integer.class, donationItemId);
    }

    private static List<Long> organizationIds(ResultActions result) throws Exception {
        List<Number> ids = JsonPath.read(result.andReturn().getResponse().getContentAsString(),
                "$.data[*].organizationId");
        return ids.stream().map(Number::longValue).toList();
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, Long userId, UserRole role) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + accessTokenProvider.issue(userId, role)));
    }

    private Object[] allMemberIds() {
        List<Long> ids = new ArrayList<>(donorIds);
        ids.addAll(organizationIds);
        return ids.toArray();
    }

    private String placeholders() {
        return placeholders(List.of(allMemberIds()));
    }

    private static String placeholders(List<?> ids) {
        return String.join(", ", ids.stream().map(id -> "?").toList());
    }
}
