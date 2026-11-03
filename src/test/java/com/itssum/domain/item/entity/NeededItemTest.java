package com.itssum.domain.item.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.domain.user.entity.Organization;
import org.junit.jupiter.api.Test;

class NeededItemTest {

    @Test
    void referenceImageCanBeReplacedAndCleared() {
        NeededItem item = item(mock(Organization.class));

        item.changeReferenceImage("needed-items/2/b.jpg");
        assertEquals("needed-items/2/b.jpg", item.getReferenceImageUrl());
        item.changeReferenceImage(null);
        assertNull(item.getReferenceImageUrl());
    }

    @Test
    void descriptionAndReceiveAddressChange() {
        NeededItem item = item(mock(Organization.class));

        item.changeDescription("평일 오후에 받아요");
        item.changeReceiveAddress("경기 부천시 길주로 1");

        assertEquals("평일 오후에 받아요", item.getDescription());
        assertEquals("경기 부천시 길주로 1", item.getReceiveAddress());
    }

    @Test
    void ownershipAndDeletion() {
        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(2L);
        NeededItem item = item(organization);

        assertTrue(item.isOwnedBy(2L));
        assertFalse(item.isOwnedBy(1L));
        item.delete();
        assertTrue(item.isDeleted());
    }

    private static NeededItem item(Organization organization) {
        return NeededItem.builder()
                .organization(organization)
                .category(mock(ItemCategory.class))
                .region(mock(Region.class))
                .itemName("생수")
                .requiredQuantity(20)
                .unit("병")
                .desiredCondition(ConditionGrade.HIGH)
                .referenceImageUrl("needed-items/2/a.jpg")
                .build();
    }
}
