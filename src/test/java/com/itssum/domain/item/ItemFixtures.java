package com.itssum.domain.item;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.ItemCategory;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

/** 물품 단위 테스트용 엔티티 (DB 없이 ID 를 직접 넣는다). */
public final class ItemFixtures {

    public static final String PICKUP_ADDRESS = "경기 부천시 길주로 210";

    private ItemFixtures() {
    }

    public static Region pilotRegion() {
        Region region = Region.builder().sido("경기").sigungu("부천시").build();
        ReflectionTestUtils.setField(region, "id", Region.PILOT_REGION_ID);
        return region;
    }

    public static ItemCategory category(Long id, String name) {
        ItemCategory category = ItemCategory.builder().name(name).sortOrder(id.intValue()).build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    public static Donor donor(Long id) {
        User user = User.builder().email("donor" + id + "@example.com").password("hashed").role(UserRole.DONOR).build();
        ReflectionTestUtils.setField(user, "id", id);
        Donor donor = Donor.builder().user(user).region(pilotRegion()).name("최윤서")
                .birthDate(LocalDate.of(2000, 1, 1)).phone("010-1234-5678").introduction("이웃에게 물품을 전해요").build();
        ReflectionTestUtils.setField(donor, "id", id);
        return donor;
    }

    public static Organization organization(Long id) {
        User user = User.builder().email("org" + id + "@example.com").password("hashed")
                .role(UserRole.ORGANIZATION).build();
        ReflectionTestUtils.setField(user, "id", id);
        Organization organization = Organization.builder().user(user).name("구로지역아동센터").representativeName("김수현")
                .businessNumber("1234567890").openingDate(LocalDate.of(2015, 3, 2)).phone("02-1234-5678")
                .address("서울 구로구 가마산로 12").addressDetail("2층").introduction("아이들의 일상을 함께해요")
                .verifiedAt(LocalDateTime.of(2026, 10, 31, 10, 0)).build();
        ReflectionTestUtils.setField(organization, "id", id);
        return organization;
    }

    public static DonationItem donationItem(Long id, Donor donor, List<String> imageKeys) {
        DonationItem item = DonationItem.builder().donor(donor).category(category(1L, "생활용품")).region(pilotRegion())
                .pickupAddress(PICKUP_ADDRESS).pickupAddressDetail("2층").itemName("수건").quantity(30).unit("장")
                .conditionGrade(ConditionGrade.MID).description("깨끗한 수건이에요").build();
        item.replaceImages(imageKeys);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    public static NeededItem neededItem(Long id, Organization organization) {
        NeededItem item = NeededItem.builder().organization(organization).category(category(1L, "생활용품"))
                .region(pilotRegion()).itemName("생수").requiredQuantity(20).unit("병")
                .desiredCondition(ConditionGrade.HIGH).description("아이들이 마실 생수가 필요해요")
                .referenceImageUrl("needed-items/" + organization.getId() + "/ref.jpg")
                .receiveAddress("서울 구로구 가마산로 12").build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
