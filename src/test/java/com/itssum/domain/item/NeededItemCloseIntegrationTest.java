package com.itssum.domain.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 모집 종료 → 응답 대기 정리(기부자 제안 거절·알림, 기관 요청 철회) → 남은 거래중 예약·예약중 수령 확인 →
 * 새 제안 막힘 → 탐색에서 빠짐 → 다시 종료까지 실제 DB 로 확인한다. 만든 행은 회원 ID 로 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NeededItemCloseIntegrationTest {

    private static final long CATEGORY_ID = 5L;
    private static final String UNIT = "권";

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
    private Long donorId;
    private Long organizationId;
    private Long donationItemId;
    private Long neededItemId;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            User donorUser = userRepository.save(User.builder().email("close-donor-" + token + "@example.com")
                    .password("test").role(UserRole.DONOR).build());
            donorRepository.save(Donor.builder().user(donorUser).name("기부자" + token)
                    .birthDate(LocalDate.of(2000, 1, 1)).phone("010-1234-5678").build());
            donorId = donorUser.getId();
            User organizationUser = userRepository.save(User.builder().email("close-org-" + token + "@example.com")
                    .password("test").role(UserRole.ORGANIZATION).build());
            organizationRepository.save(Organization.builder().user(organizationUser).name("센터" + token)
                    .representativeName("김수현")
                    .businessNumber(String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L)))
                    .openingDate(LocalDate.of(2015, 3, 2)).phone("032-123-4567").address("경기 부천시 길주로 210")
                    .verifiedAt(LocalDateTime.now()).build());
            organizationId = organizationUser.getId();
        });
        donationItemId = transactionTemplate.execute(status -> donationItemRepository.save(DonationItem.builder()
                .donor(donorRepository.getReferenceById(donorId))
                .category(itemCategoryRepository.getReferenceById(CATEGORY_ID))
                .region(regionRepository.getReferenceById(Region.PILOT_REGION_ID)).pickupAddress("경기 부천시 길주로 210")
                .itemName("동화책").quantity(30).unit(UNIT).conditionGrade(ConditionGrade.HIGH).build()).getId());
        neededItemId = transactionTemplate.execute(status -> neededItemRepository.save(NeededItem.builder()
                .organization(organizationRepository.getReferenceById(organizationId))
                .category(itemCategoryRepository.getReferenceById(CATEGORY_ID))
                .region(regionRepository.getReferenceById(Region.PILOT_REGION_ID)).itemName("모집종료" + token)
                .requiredQuantity(20).unit(UNIT).desiredCondition(ConditionGrade.MID).build()).getId());
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from notifications where receiver_id in (?, ?)", donorId, organizationId);
        jdbcTemplate.update("delete from matches where needed_item_id = ?", neededItemId);
        jdbcTemplate.update("delete from donation_items where donor_id = ?", donorId);
        jdbcTemplate.update("delete from needed_items where organization_id = ?", organizationId);
        jdbcTemplate.update("delete from donors where user_id = ?", donorId);
        jdbcTemplate.update("delete from organizations where user_id = ?", organizationId);
        jdbcTemplate.update("delete from users where user_id in (?, ?)", donorId, organizationId);
    }

    @Test
    void closeCleansPendingAndLetsOngoingTradesFinish() throws Exception {
        Long proposal = match(RequesterType.DONOR, "PENDING");
        Long request = match(RequesterType.ORGANIZATION, "PENDING");
        Long trading = match(RequesterType.DONOR, "TRADING");
        Long reserved = match(RequesterType.DONOR, "RESERVED");
        jdbcTemplate.update("update donation_items set reserved_quantity = 5 where donation_item_id = ?", donationItemId);
        jdbcTemplate.update("update needed_items set reserved_quantity = 5 where needed_item_id = ?", neededItemId);
        String closePath = "/api/needed-items/" + neededItemId + "/close";

        // 1. 종료
        perform(patch(closePath), organizationId, UserRole.ORGANIZATION, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_14"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // 2. 응답 대기만 정리되고 거래중·예약중은 그대로
        assertEquals("REJECTED", matchStatus(proposal));
        assertEquals("CANCELED", matchStatus(request));
        assertEquals("TRADING", matchStatus(trading));
        assertEquals("RESERVED", matchStatus(reserved));

        // 3. 기부자에게 거절 알림은 제안 한 건에만 간다 (기관이 보낸 요청의 철회는 알림 없음)
        assertEquals(1, rejectedNotifications());

        // 4·5. 종료 뒤에도 거래중은 예약, 예약중은 수령 확인까지 진행된다
        perform(patch("/api/matches/" + trading + "/reserve"), donorId, UserRole.DONOR, null)
                .andExpect(status().isOk());
        perform(patch("/api/matches/" + reserved + "/complete"), organizationId, UserRole.ORGANIZATION, null)
                .andExpect(status().isOk());
        assertEquals("CLOSED", jdbcTemplate.queryForObject(
                "select status from needed_items where needed_item_id = ?", String.class, neededItemId));

        // 6. 새 제안은 막힌다
        perform(post("/api/matches"), donorId, UserRole.DONOR, """
                {"donationItemId": %d, "neededItemId": %d, "quantity": 1}
                """.formatted(donationItemId, neededItemId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MATCH_400"));

        // 7. 탐색에서 빠진다
        perform(get("/api/needed-items").param("keyword", "모집종료" + token), donorId, UserRole.DONOR, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));

        // 8. 다시 종료해도 200이고 아무것도 바뀌지 않는다
        LocalDateTime closedAt = closedAt();
        perform(patch(closePath), organizationId, UserRole.ORGANIZATION, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
        assertEquals(closedAt, closedAt());
        assertEquals(1, rejectedNotifications());
    }

    private Long match(RequesterType requestedBy, String status) {
        Long matchId = transactionTemplate.execute(tx -> matchRepository.save(Match.builder()
                .donationItem(donationItemRepository.getReferenceById(donationItemId))
                .neededItem(neededItemRepository.getReferenceById(neededItemId))
                .donor(donorRepository.getReferenceById(donorId))
                .organization(organizationRepository.getReferenceById(organizationId))
                .requestedBy(requestedBy).quantity(5).build()).getId());
        jdbcTemplate.update("update matches set status = ? where match_id = ?", status, matchId);
        return matchId;
    }

    private String matchStatus(Long matchId) {
        return jdbcTemplate.queryForObject("select status from matches where match_id = ?", String.class, matchId);
    }

    private int rejectedNotifications() {
        return jdbcTemplate.queryForObject(
                "select count(*) from notifications where receiver_id = ? and type = 'MATCH_REJECTED'",
                Integer.class, donorId);
    }

    private LocalDateTime closedAt() {
        return jdbcTemplate.queryForObject("select closed_at from needed_items where needed_item_id = ?",
                LocalDateTime.class, neededItemId);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, Long userId, UserRole role, String body)
            throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenProvider.issue(userId, role));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }
}
