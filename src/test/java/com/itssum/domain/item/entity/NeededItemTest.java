package com.itssum.domain.item.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.domain.user.entity.Organization;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NeededItemTest {

    private static final LocalDateTime FIRST_CLOSED_AT = LocalDateTime.of(2026, 11, 1, 10, 0);

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

    @Test
    void closeStopsRecruiting() {
        NeededItem item = item(mock(Organization.class));

        item.close();

        assertEquals(NeededItemStatus.CLOSED, item.getStatus());
        assertNotNull(item.getClosedAt());
    }

    @Test
    void closeFromFull() {
        NeededItem item = item(mock(Organization.class));
        item.reserve(20);
        assertEquals(NeededItemStatus.FULL, item.getStatus());

        item.close();

        assertEquals(NeededItemStatus.CLOSED, item.getStatus());
    }

    @Test
    void closeTwiceKeepsFirstClosedAt() {
        NeededItem item = item(mock(Organization.class));
        item.close();
        ReflectionTestUtils.setField(item, "closedAt", FIRST_CLOSED_AT);

        item.close();

        assertEquals(FIRST_CLOSED_AT, item.getClosedAt());
    }

    @Test
    void completingAfterManualCloseKeepsClosedAt() {
        NeededItem item = item(mock(Organization.class));
        item.reserve(20);
        item.close();
        ReflectionTestUtils.setField(item, "closedAt", FIRST_CLOSED_AT);

        item.complete(20);

        assertEquals(NeededItemStatus.CLOSED, item.getStatus());
        assertEquals(FIRST_CLOSED_AT, item.getClosedAt());
    }

    @Test
    void releaseAfterManualCloseStaysClosed() {
        NeededItem item = item(mock(Organization.class));
        item.reserve(5);
        item.close();

        item.release(5);

        assertEquals(NeededItemStatus.CLOSED, item.getStatus());
        assertEquals(20, item.availableQuantity());
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
