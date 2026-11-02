package com.itssum.domain.item;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.item.image.ItemImageStorage;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.item.repository.NeededItemRepository;
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
import com.itssum.global.infra.kakao.GeoPoint;
import com.itssum.global.infra.kakao.KakaoLocalClient;
import com.itssum.global.security.AccessTokenProvider;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 기준 데이터 조회 → 기부 물품·필요 물품 등록 → 탐색·상세 → 수정 → 진행 중 매칭이 있을 때 삭제 막힘 → 삭제까지 실제 DB 로 확인한다.
 * S3 와 카카오는 대체하고, 만든 행은 회원 ID 로 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemFlowIntegrationTest {

    private static final GeoPoint POINT = new GeoPoint(new BigDecimal("37.5034138"), new BigDecimal("126.7660309"));

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
    private MatchRepository matchRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ItemImageStorage itemImageStorage;

    @MockitoBean
    private KakaoLocalClient kakaoLocalClient;

    private final String token = UUID.randomUUID().toString().substring(0, 8);
    private Long donorId;
    private Long organizationId;

    @BeforeEach
    void setUp() {
        when(itemImageStorage.isUploaded(any(), anyLong(), anyString())).thenReturn(true);
        when(itemImageStorage.presignRead(anyString()))
                .thenAnswer(invocation -> "https://example.com/" + invocation.getArgument(0));
        when(kakaoLocalClient.geocode(anyString())).thenReturn(Optional.of(POINT));
        transactionTemplate.executeWithoutResult(status -> {
            User donorUser = userRepository.save(User.builder().email("item-flow-donor-" + token + "@example.com")
                    .password("test").role(UserRole.DONOR).build());
            donorRepository.save(Donor.builder().user(donorUser).name("기부자" + token)
                    .birthDate(LocalDate.of(2000, 1, 1)).phone("010-1234-5678").build());
            donorId = donorUser.getId();
            User organizationUser = userRepository.save(User.builder().email("item-flow-org-" + token + "@example.com")
                    .password("test").role(UserRole.ORGANIZATION).build());
            organizationRepository.save(Organization.builder().user(organizationUser).name("센터" + token)
                    .representativeName("김수현")
                    .businessNumber(String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L)))
                    .openingDate(LocalDate.of(2015, 3, 2)).phone("02-1234-5678").address("서울 구로구 가마산로 12")
                    .verifiedAt(LocalDateTime.now()).build());
            organizationId = organizationUser.getId();
        });
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from matches where donor_id = ? or organization_id = ?", donorId, organizationId);
        jdbcTemplate.update("delete i from donation_item_images i join donation_items d "
                + "on i.donation_item_id = d.donation_item_id where d.donor_id = ?", donorId);
        jdbcTemplate.update("delete from donation_items where donor_id = ?", donorId);
        jdbcTemplate.update("delete from needed_items where organization_id = ?", organizationId);
        jdbcTemplate.update("delete from donors where user_id = ?", donorId);
        jdbcTemplate.update("delete from organizations where user_id = ?", organizationId);
        jdbcTemplate.update("delete from users where user_id in (?, ?)", donorId, organizationId);
    }

    @Test
    void registerBrowseUpdateAndDeleteItems() throws Exception {
        // 기준 데이터는 로그인 없이
        mockMvc.perform(get("/api/item-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(7))
                .andExpect(jsonPath("$.data[4].name").value("도서·문구"));
        mockMvc.perform(get("/api/item-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(9));

        // 기부자 등록 → 좌표 저장, 기관 등록
        String imageKey = "donation-items/" + donorId + "/" + UUID.randomUUID() + ".jpg";
        ResultActions created = perform(post("/api/donation-items"), donorId, UserRole.DONOR, """
                {"categoryId": 5, "itemName": "동화책%s", "quantity": 30, "unit": "권", "conditionGrade": "MID",
                 "imageKeys": ["%s"], "pickupAddress": "경기 부천시 길주로 210", "pickupAddressDetail": "2층"}
                """.formatted(token, imageKey))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ITEM_201"));
        Long donationItemId = id(created, "$.data.donationItemId");
        assertEquals(0, POINT.latitude().compareTo(jdbcTemplate.queryForObject(
                "select latitude from donation_items where donation_item_id = ?", BigDecimal.class, donationItemId)));
        Long neededItemId = id(perform(post("/api/needed-items"), organizationId, UserRole.ORGANIZATION, """
                {"categoryId": 5, "itemName": "어린이 도서%s", "requiredQuantity": 20, "unit": "권",
                 "desiredCondition": "MID", "description": "아이들이 읽을 책이 필요해요"}
                """.formatted(token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ITEM_201_1")), "$.data.neededItemId");

        // 기관 탐색·상세: 상세 위치는 숨김
        perform(get("/api/donation-items").param("keyword", token), organizationId, UserRole.ORGANIZATION, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].donationItemId").value(donationItemId))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("https://example.com/" + imageKey));
        perform(get("/api/donation-items/" + donationItemId), organizationId, UserRole.ORGANIZATION, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mine").value(false))
                .andExpect(jsonPath("$.data.pickupAddress").doesNotExist())
                .andExpect(jsonPath("$.data.donorName").value("기부자" + token));
        perform(get("/api/donation-items/" + donationItemId), donorId, UserRole.DONOR, null)
                .andExpect(jsonPath("$.data.pickupAddress").value("경기 부천시 길주로 210"))
                .andExpect(jsonPath("$.data.imageKeys[0]").value(imageKey));

        // 기부자 수정 → 내 목록·상세 반영
        perform(patch("/api/donation-items/" + donationItemId), donorId, UserRole.DONOR, "{\"description\": \"새 설명\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("새 설명"));
        perform(get("/api/donors/me/donation-items"), donorId, UserRole.DONOR, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].donationItemId").value(donationItemId))
                .andExpect(jsonPath("$.data.items[0].imageUrl", startsWith("https://example.com/donation-items/")));

        // 사진 교체: 새 사진을 대표로, 기존 사진은 두 번째로 → DB 행과 목록 대표 사진이 바뀜
        String newKey = "donation-items/" + donorId + "/" + UUID.randomUUID() + ".jpg";
        perform(patch("/api/donation-items/" + donationItemId), donorId, UserRole.DONOR,
                "{\"imageKeys\": [\"%s\", \"%s\"]}".formatted(newKey, imageKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageKeys[0]").value(newKey))
                .andExpect(jsonPath("$.data.imageKeys[1]").value(imageKey));
        assertEquals(List.of(newKey, imageKey), jdbcTemplate.queryForList(
                "select image_url from donation_item_images where donation_item_id = ? order by sort_order",
                String.class, donationItemId));
        perform(get("/api/donation-items").param("keyword", token), organizationId, UserRole.ORGANIZATION, null)
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("https://example.com/" + newKey));

        // 기부자 탐색·상세: 수령 장소·참고 사진 key 는 숨김
        perform(get("/api/needed-items").param("keyword", token), donorId, UserRole.DONOR, null)
                .andExpect(jsonPath("$.data.items[*].neededItemId", hasItem(neededItemId.intValue())));
        perform(get("/api/needed-items/" + neededItemId), donorId, UserRole.DONOR, null)
                .andExpect(jsonPath("$.data.organizationName").value("센터" + token))
                .andExpect(jsonPath("$.data.referenceImageKey").doesNotExist());

        // 응답 대기 매칭이 있으면 두 물품 모두 삭제 불가
        Long matchId = transactionTemplate.execute(status -> matchRepository.save(Match.builder()
                .donationItem(donationItemRepository.findById(donationItemId).orElseThrow())
                .neededItem(neededItemRepository.findById(neededItemId).orElseThrow())
                .donor(donorRepository.findById(donorId).orElseThrow())
                .organization(organizationRepository.findById(organizationId).orElseThrow())
                .requestedBy(RequesterType.DONOR).quantity(10).build()).getId());
        perform(delete("/api/donation-items/" + donationItemId), donorId, UserRole.DONOR, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ITEM_409"));
        perform(delete("/api/needed-items/" + neededItemId), organizationId, UserRole.ORGANIZATION, null)
                .andExpect(status().isConflict());

        // 매칭이 끝나면 삭제 → 상세 404, 탐색에서 빠짐
        jdbcTemplate.update("update matches set status = 'REJECTED' where match_id = ?", matchId);
        perform(delete("/api/donation-items/" + donationItemId), donorId, UserRole.DONOR, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_7"));
        perform(get("/api/donation-items/" + donationItemId), organizationId, UserRole.ORGANIZATION, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITEM_404"));
        perform(get("/api/donation-items").param("keyword", token), organizationId, UserRole.ORGANIZATION, null)
                .andExpect(jsonPath("$.data.items[*].donationItemId", not(hasItem(donationItemId.intValue()))));
        perform(delete("/api/needed-items/" + neededItemId), organizationId, UserRole.ORGANIZATION, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ITEM_200_12"));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, Long userId, UserRole role, String body)
            throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenProvider.issue(userId, role));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private static Long id(ResultActions result, String path) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), path)).longValue();
    }
}
