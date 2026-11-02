package com.itssum.domain.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itssum.domain.item.dto.response.DonationItemSummaryResponse;
import com.itssum.domain.item.dto.response.MyDonationItemResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.item.repository.ItemCategoryRepository;
import com.itssum.domain.item.repository.RegionRepository;
import com.itssum.domain.item.service.DonationItemQueryService;
import com.itssum.domain.item.service.ItemSearchCondition;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.repository.DonorRepository;
import com.itssum.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 기부 물품 탐색·내 목록 쿼리를 실제 DB 로 확인한다. 다른 테스트의 물품과 섞이지 않도록 품목·기부자 이름에 고유 토큰을 넣고,
 * 만든 행은 기부자 ID 로 지운다.
 */
@SpringBootTest
class DonationItemSearchIntegrationTest {

    @Autowired
    private DonationItemQueryService queryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private DonationItemRepository donationItemRepository;

    @Autowired
    private ItemCategoryRepository categoryRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String token = UUID.randomUUID().toString().substring(0, 8);
    private final List<Long> donorIds = new ArrayList<>();
    private Long donorId;

    @BeforeEach
    void setUp() {
        donorId = createDonor("기부자" + token);
    }

    @AfterEach
    void cleanUp() {
        for (Long id : donorIds) {
            jdbcTemplate.update("delete i from donation_item_images i join donation_items d "
                    + "on i.donation_item_id = d.donation_item_id where d.donor_id = ?", id);
            jdbcTemplate.update("delete from donation_items where donor_id = ?", id);
            jdbcTemplate.update("delete from donors where user_id = ?", id);
            jdbcTemplate.update("delete from users where user_id = ?", id);
        }
    }

    @Test
    void browseShowsOnlyRecruitingAndNotDeleted() {
        Long recruiting = createItem("수건" + token, 1L, ConditionGrade.MID, item -> { });
        createItem("담요" + token, 1L, ConditionGrade.MID, item -> item.reserve(item.getQuantity()));
        createItem("베개" + token, 1L, ConditionGrade.MID, DonationItem::delete);

        assertEquals(List.of(recruiting), browse(new ItemSearchCondition(token, null, null, null)));
    }

    @Test
    void keywordMatchesItemNameOrDonorNameLiterally() {
        Long percent = createItem("100% 면 수건" + token, 1L, ConditionGrade.MID, item -> { });
        Long thousand = createItem("1000 면 수건" + token, 1L, ConditionGrade.MID, item -> { });

        List<Long> found = browse(new ItemSearchCondition("100%", null, null, null));
        assertTrue(found.contains(percent));
        assertTrue(!found.contains(thousand));

        Long other = createItem("장난감", 6L, ConditionGrade.HIGH, item -> { });
        List<Long> byDonor = browse(new ItemSearchCondition("기부자" + token, null, null, null));
        assertEquals(List.of(other, thousand, percent), byDonor);
    }

    @Test
    void filtersByCategoryRegionAndCondition() {
        Long book = createItem("동화책" + token, 5L, ConditionGrade.HIGH, item -> { });
        Long towel = createItem("수건" + token, 1L, ConditionGrade.MID, item -> { });

        assertEquals(List.of(book), browse(new ItemSearchCondition(token, 5L, null, null)));
        assertEquals(List.of(towel), browse(new ItemSearchCondition(token, null, null, ConditionGrade.MID)));
        assertEquals(List.of(towel, book), browse(new ItemSearchCondition(token, null, Region.PILOT_REGION_ID, null)));
        assertEquals(List.of(), browse(new ItemSearchCondition(token, null, 999L, null)));
    }

    @Test
    void mineIncludesAllStatusesNewestFirstWithPaging() {
        Long recruiting = createItem("수건" + token, 1L, ConditionGrade.MID, item -> { });
        Long full = createItem("담요" + token, 1L, ConditionGrade.MID, item -> item.reserve(item.getQuantity()));
        Long completed = createItem("동화책" + token, 5L, ConditionGrade.HIGH, item -> {
            item.reserve(item.getQuantity());
            item.complete(item.getQuantity());
        });
        createItem("베개" + token, 1L, ConditionGrade.MID, DonationItem::delete);

        var first = queryService.mine(donorId, null, null, 2);
        assertEquals(List.of(completed, full), first.items().stream().map(MyDonationItemResponse::donationItemId).toList());
        assertTrue(first.hasNext());
        var second = queryService.mine(donorId, null, first.nextCursor(), 2);
        assertEquals(List.of(recruiting), second.items().stream().map(MyDonationItemResponse::donationItemId).toList());
        assertTrue(!second.hasNext());

        var books = queryService.mine(donorId, 5L, null, 20);
        assertEquals(List.of(completed), books.items().stream().map(MyDonationItemResponse::donationItemId).toList());
        assertEquals("COMPLETED", books.items().getFirst().status().name());
        assertEquals("도서·문구", books.items().getFirst().categoryName());
    }

    private List<Long> browse(ItemSearchCondition condition) {
        return queryService.browse(donorId, condition, null, 50).items().stream()
                .map(DonationItemSummaryResponse::donationItemId)
                .filter(id -> donationItemRepository.findById(id)
                        .map(item -> donorIds.contains(item.getDonor().getId())).orElse(false))
                .toList();
    }

    private Long createDonor(String name) {
        Long id = transactionTemplate.execute(status -> {
            User user = userRepository.save(User.builder().email("item-search-" + UUID.randomUUID() + "@example.com")
                    .password("test").role(UserRole.DONOR).build());
            Region region = regionRepository.findById(Region.PILOT_REGION_ID).orElseThrow();
            donorRepository.save(Donor.builder().user(user).region(region).name(name)
                    .birthDate(LocalDate.of(2000, 1, 1)).phone("010-1234-5678").build());
            return user.getId();
        });
        donorIds.add(id);
        return id;
    }

    private Long createItem(String itemName, Long categoryId, ConditionGrade grade, Consumer<DonationItem> change) {
        return transactionTemplate.execute(status -> {
            DonationItem item = DonationItem.builder()
                    .donor(donorRepository.findById(donorId).orElseThrow())
                    .category(categoryRepository.findById(categoryId).orElseThrow())
                    .region(regionRepository.findById(Region.PILOT_REGION_ID).orElseThrow())
                    .pickupAddress("경기 부천시 길주로 210")
                    .itemName(itemName)
                    .quantity(10)
                    .unit("개")
                    .conditionGrade(grade)
                    .build();
            item.replaceImages(List.of("donation-items/" + donorId + "/" + UUID.randomUUID() + ".jpg"));
            change.accept(item);
            return donationItemRepository.save(item).getId();
        });
    }
}
