package com.itssum.domain.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itssum.domain.item.dto.response.MyNeededItemResponse;
import com.itssum.domain.item.dto.response.NeededItemSummaryResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.repository.ItemCategoryRepository;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.item.repository.RegionRepository;
import com.itssum.domain.item.service.ItemSearchCondition;
import com.itssum.domain.item.service.NeededItemQueryService;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.repository.OrganizationRepository;
import com.itssum.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 필요 물품 탐색·우리 기관 목록 쿼리를 실제 DB 로 확인한다. 품목·기관명에 고유 토큰을 넣어 다른 테스트 데이터와 섞이지 않게 하고,
 * 만든 행은 기관 ID 로 지운다.
 */
@SpringBootTest
class NeededItemSearchIntegrationTest {

    @Autowired
    private NeededItemQueryService queryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private NeededItemRepository neededItemRepository;

    @Autowired
    private ItemCategoryRepository categoryRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String token = UUID.randomUUID().toString().substring(0, 8);
    private final List<Long> organizationIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long id : organizationIds) {
            jdbcTemplate.update("delete from needed_items where organization_id = ?", id);
            jdbcTemplate.update("delete from organizations where user_id = ?", id);
            jdbcTemplate.update("delete from users where user_id = ?", id);
        }
    }

    @Test
    void suspendedOrganizationItemsAreHidden() {
        Long verified = createOrganization("센터" + token);
        Long suspended = createOrganization("휴업센터" + token);
        jdbcTemplate.update("update organizations set verification_status = 'SUSPENDED' where user_id = ?", suspended);
        Long shown = createItem(verified, "생수" + token, 1L, item -> { });
        createItem(suspended, "수건" + token, 1L, item -> { });

        assertEquals(List.of(shown), browse(new ItemSearchCondition(token, null, null, null)));
    }

    @Test
    void closedFullAndDeletedAreHidden() {
        Long organizationId = createOrganization("센터" + token);
        Long recruiting = createItem(organizationId, "생수" + token, 1L, item -> { });
        createItem(organizationId, "수건" + token, 1L, item -> item.reserve(item.getRequiredQuantity()));
        createItem(organizationId, "담요" + token, 1L, item -> {
            item.reserve(item.getRequiredQuantity());
            item.complete(item.getRequiredQuantity());
        });
        createItem(organizationId, "베개" + token, 1L, NeededItem::delete);

        assertEquals(List.of(recruiting), browse(new ItemSearchCondition(token, null, null, null)));
    }

    @Test
    void keywordMatchesOrganizationNameLiterally() {
        Long underscore = createOrganization("A_센터" + token);
        Long similar = createOrganization("AB센터" + token);
        Long first = createItem(underscore, "생수", 1L, item -> { });
        Long second = createItem(similar, "생수", 1L, item -> { });

        List<Long> found = browse(new ItemSearchCondition("A_센터", null, null, null));
        assertTrue(found.contains(first));
        assertTrue(!found.contains(second));
    }

    @Test
    void filtersByCategoryAndDesiredCondition() {
        Long organizationId = createOrganization("센터" + token);
        Long water = createItem(organizationId, "생수" + token, 1L, item -> { });
        Long book = createItem(organizationId, "동화책" + token, 5L, item -> { });

        assertEquals(List.of(book), browse(new ItemSearchCondition(token, 5L, null, null)));
        assertEquals(List.of(book, water), browse(new ItemSearchCondition(token, null, null, ConditionGrade.HIGH)));
        assertEquals(List.of(), browse(new ItemSearchCondition(token, null, null, ConditionGrade.LOW)));
    }

    @Test
    void mineIncludesClosedNewestFirst() {
        Long organizationId = createOrganization("센터" + token);
        Long recruiting = createItem(organizationId, "생수" + token, 1L, item -> { });
        Long closed = createItem(organizationId, "담요" + token, 1L, item -> {
            item.reserve(item.getRequiredQuantity());
            item.complete(item.getRequiredQuantity());
        });
        createItem(organizationId, "베개" + token, 1L, NeededItem::delete);

        var page = queryService.mine(organizationId, null, null, 20);

        assertEquals(List.of(closed, recruiting), page.items().stream().map(MyNeededItemResponse::neededItemId).toList());
        assertEquals("CLOSED", page.items().getFirst().status().name());
        assertEquals(10, page.items().getFirst().receivedQuantity());
    }

    private List<Long> browse(ItemSearchCondition condition) {
        return queryService.browse(organizationIds.getFirst(), condition, null, 50).items().stream()
                .map(NeededItemSummaryResponse::neededItemId)
                .filter(id -> neededItemRepository.findById(id)
                        .map(item -> organizationIds.contains(item.getOrganization().getId())).orElse(false))
                .toList();
    }

    private Long createOrganization(String name) {
        Long id = transactionTemplate.execute(status -> {
            User user = userRepository.save(User.builder().email("needed-search-" + UUID.randomUUID() + "@example.com")
                    .password("test").role(UserRole.ORGANIZATION).build());
            organizationRepository.save(Organization.builder().user(user).name(name).representativeName("김수현")
                    .businessNumber(String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L)))
                    .openingDate(LocalDate.of(2015, 3, 2)).phone("02-1234-5678").address("서울 구로구 가마산로 12")
                    .verifiedAt(LocalDateTime.now()).build());
            return user.getId();
        });
        organizationIds.add(id);
        return id;
    }

    private Long createItem(Long organizationId, String itemName, Long categoryId, Consumer<NeededItem> change) {
        return transactionTemplate.execute(status -> {
            NeededItem item = NeededItem.builder()
                    .organization(organizationRepository.findById(organizationId).orElseThrow())
                    .category(categoryRepository.findById(categoryId).orElseThrow())
                    .region(regionRepository.findById(Region.PILOT_REGION_ID).orElseThrow())
                    .itemName(itemName)
                    .requiredQuantity(10)
                    .unit("개")
                    .desiredCondition(ConditionGrade.HIGH)
                    .build();
            change.accept(item);
            return neededItemRepository.save(item).getId();
        });
    }
}
