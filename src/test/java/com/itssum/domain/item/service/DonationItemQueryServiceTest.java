package com.itssum.domain.item.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itssum.domain.item.ItemFixtures;
import com.itssum.domain.item.dto.response.DonationItemDetailResponse;
import com.itssum.domain.item.dto.response.DonationItemSummaryResponse;
import com.itssum.domain.item.dto.response.ItemPageResponse;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.DonationItemImageRepository;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class DonationItemQueryServiceTest {

    private static final Long DONOR_ID = 1L;
    private static final Long ORGANIZATION_ID = 2L;
    private static final ItemSearchCondition NO_FILTER = new ItemSearchCondition(null, null, null, null);

    private final MemberReader memberReader = mock(MemberReader.class);
    private final DonationItemRepository donationItemRepository = mock(DonationItemRepository.class);
    private final DonationItemImageRepository imageRepository = mock(DonationItemImageRepository.class);
    private final ItemImages itemImages = mock(ItemImages.class);
    private final DonationItemQueryService service =
            new DonationItemQueryService(memberReader, donationItemRepository, imageRepository, itemImages);

    private DonationItem item;

    @BeforeEach
    void setUp() {
        Donor donor = ItemFixtures.donor(DONOR_ID);
        item = ItemFixtures.donationItem(10L, donor, List.of("donation-items/1/a.jpg", "donation-items/1/b.jpg"));
        when(donationItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemImages.readUrl(any())).thenAnswer(invocation -> "https://example.com/" + invocation.getArgument(0));
        when(imageRepository.findByDonationItemIdInAndSortOrder(anyCollection(), anyInt())).thenReturn(List.of());
    }

    @Test
    void detailHidesPrivateFieldsFromOthers() {
        DonationItemDetailResponse response = service.detail(ORGANIZATION_ID, 10L);

        assertFalse(response.mine());
        assertNull(response.pickupAddress());
        assertNull(response.pickupAddressDetail());
        assertNull(response.imageKeys());
        assertEquals("최윤서", response.donorName());
        assertEquals("이웃에게 물품을 전해요", response.donorIntroduction());
        assertEquals(List.of("https://example.com/donation-items/1/a.jpg", "https://example.com/donation-items/1/b.jpg"),
                response.imageUrls());
    }

    @Test
    void ownerSeesPrivateFields() {
        DonationItemDetailResponse response = service.detail(DONOR_ID, 10L);

        assertTrue(response.mine());
        assertEquals(ItemFixtures.PICKUP_ADDRESS, response.pickupAddress());
        assertEquals("2층", response.pickupAddressDetail());
        assertEquals(List.of("donation-items/1/a.jpg", "donation-items/1/b.jpg"), response.imageKeys());
    }

    @Test
    void deletedItemDetailIs404() {
        item.delete();

        assertEquals(ErrorCode.ITEM_NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.detail(ORGANIZATION_ID, 10L)).getErrorCode());
        when(donationItemRepository.findById(11L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.ITEM_NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.detail(ORGANIZATION_ID, 11L)).getErrorCode());
    }

    @Test
    void fullItemDetailStillOpens() {
        item.reserve(30);

        DonationItemDetailResponse response = service.detail(ORGANIZATION_ID, 10L);

        assertEquals(0, response.availableQuantity());
        assertEquals("FULL", response.status().name());
    }

    @Test
    void withdrawnViewerIs401() {
        when(memberReader.activeUser(ORGANIZATION_ID)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        assertEquals(ErrorCode.UNAUTHORIZED,
                assertThrows(BusinessException.class, () -> service.detail(ORGANIZATION_ID, 10L)).getErrorCode());
    }

    @Test
    void sizeOutOfRangeIs400() {
        assertEquals(ErrorCode.INVALID_INPUT, assertThrows(BusinessException.class,
                () -> service.browse(ORGANIZATION_ID, NO_FILTER, null, 0)).getErrorCode());
        assertEquals(ErrorCode.INVALID_INPUT, assertThrows(BusinessException.class,
                () -> service.browse(ORGANIZATION_ID, NO_FILTER, null, 51)).getErrorCode());
        assertEquals(ErrorCode.INVALID_INPUT, assertThrows(BusinessException.class,
                () -> service.mine(DONOR_ID, null, 0L, 20)).getErrorCode());
        verifyNoInteractions(donationItemRepository);
    }

    @Test
    void hasNextWhenMoreThanSize() {
        Donor donor = ItemFixtures.donor(DONOR_ID);
        List<DonationItem> fetched = List.of(ItemFixtures.donationItem(30L, donor, List.of("k30")),
                ItemFixtures.donationItem(20L, donor, List.of("k20")),
                ItemFixtures.donationItem(10L, donor, List.of("k10")));
        when(donationItemRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(fetched);

        ItemPageResponse<DonationItemSummaryResponse> page = service.browse(ORGANIZATION_ID, NO_FILTER, null, 2);

        assertTrue(page.hasNext());
        assertEquals(20L, page.nextCursor());
        assertEquals(List.of(30L, 20L), page.items().stream().map(DonationItemSummaryResponse::donationItemId).toList());
        assertEquals(30, page.items().getFirst().availableQuantity());
        assertEquals("경기 부천시", page.items().getFirst().regionName());
    }

    @Test
    void lastPageHasNoCursor() {
        when(donationItemRepository.findMine(eq(DONOR_ID), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(item));

        var page = service.mine(DONOR_ID, null, null, 20);

        assertFalse(page.hasNext());
        assertNull(page.nextCursor());
        assertEquals(1, page.items().size());
    }

    @Test
    void likePatternEscapesWildcards() {
        assertEquals("%100!%%", new ItemSearchCondition(" 100% ", null, null, null).likePattern());
        assertEquals("%a!_b!!c%", new ItemSearchCondition("a_b!c", null, null, null).likePattern());
        assertNull(new ItemSearchCondition("　 ", null, null, null).likePattern());
        assertNull(NO_FILTER.likePattern());
    }
}
