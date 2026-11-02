package com.itssum.domain.item.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.domain.user.entity.Donor;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DonationItemTest {

    @Test
    void replaceImagesKeepsGivenOrder() {
        DonationItem item = item(mock(Donor.class));
        item.addImage("a", 0);
        item.addImage("b", 1);

        item.replaceImages(List.of("c", "a"));

        assertEquals(List.of("c", "a"), item.imageKeys());
        assertEquals("c", item.representativeImageKey());
        assertEquals(List.of(0, 1), item.getImages().stream().map(DonationItemImage::getSortOrder).toList());
    }

    @Test
    void itemWithoutImagesHasNoRepresentative() {
        assertNull(item(mock(Donor.class)).representativeImageKey());
    }

    @Test
    void ownershipAndDeletion() {
        Donor donor = mock(Donor.class);
        when(donor.getId()).thenReturn(1L);
        DonationItem item = item(donor);

        assertTrue(item.isOwnedBy(1L));
        assertFalse(item.isOwnedBy(2L));
        assertFalse(item.isDeleted());
        item.delete();
        assertTrue(item.isDeleted());
    }

    @Test
    void relocateReplacesAddressAndCoordinates() {
        DonationItem item = item(mock(Donor.class));

        item.relocate("경기 부천시 길주로 1", new BigDecimal("37.5000000"), new BigDecimal("126.8000000"));
        item.changePickupAddressDetail("2층");
        item.changeDescription(null);

        assertEquals("경기 부천시 길주로 1", item.getPickupAddress());
        assertEquals(new BigDecimal("37.5000000"), item.getLatitude());
        assertEquals("2층", item.getPickupAddressDetail());
        assertNull(item.getDescription());

        item.relocate("경기 부천시 길주로 2", null, null);
        assertNull(item.getLatitude());
        assertNull(item.getLongitude());
    }

    private static DonationItem item(Donor donor) {
        return DonationItem.builder()
                .donor(donor)
                .category(mock(ItemCategory.class))
                .region(mock(Region.class))
                .pickupAddress("경기 부천시 길주로 0")
                .itemName("수건")
                .quantity(30)
                .unit("장")
                .conditionGrade(ConditionGrade.MID)
                .description("설명")
                .build();
    }
}
