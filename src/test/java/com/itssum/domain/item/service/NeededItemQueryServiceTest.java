package com.itssum.domain.item.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.domain.item.ItemFixtures;
import com.itssum.domain.item.dto.response.NeededItemDetailResponse;
import com.itssum.domain.item.dto.response.NeededItemSummaryResponse;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.VerificationStatus;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class NeededItemQueryServiceTest {

    private static final Long DONOR_ID = 1L;
    private static final Long ORGANIZATION_ID = 2L;

    private final MemberReader memberReader = mock(MemberReader.class);
    private final NeededItemRepository neededItemRepository = mock(NeededItemRepository.class);
    private final ItemImages itemImages = mock(ItemImages.class);
    private final NeededItemQueryService service = new NeededItemQueryService(memberReader, neededItemRepository, itemImages);

    private NeededItem item;

    @BeforeEach
    void setUp() {
        Organization organization = ItemFixtures.organization(ORGANIZATION_ID);
        item = ItemFixtures.neededItem(20L, organization);
        when(neededItemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(itemImages.readUrl(any())).thenAnswer(invocation ->
                invocation.getArgument(0) == null ? null : "https://example.com/" + invocation.getArgument(0));
    }

    @Test
    void detailHidesReceiveAddressAndImageKeyFromDonor() {
        NeededItemDetailResponse response = service.detail(DONOR_ID, 20L);

        assertFalse(response.mine());
        assertNull(response.receiveAddress());
        assertNull(response.referenceImageKey());
        assertEquals("https://example.com/needed-items/2/ref.jpg", response.referenceImageUrl());
    }

    @Test
    void ownerSeesReceiveAddressAndImageKey() {
        NeededItemDetailResponse response = service.detail(ORGANIZATION_ID, 20L);

        assertTrue(response.mine());
        assertEquals("서울 구로구 가마산로 12", response.receiveAddress());
        assertEquals("needed-items/2/ref.jpg", response.referenceImageKey());
    }

    @Test
    void detailShowsOrganizationAddressAndVerification() {
        NeededItemDetailResponse response = service.detail(DONOR_ID, 20L);

        assertEquals("구로지역아동센터", response.organizationName());
        assertEquals(VerificationStatus.VERIFIED, response.verificationStatus());
        assertEquals("서울 구로구 가마산로 12", response.organizationAddress());
        assertEquals("아이들의 일상을 함께해요", response.organizationIntroduction());
        assertEquals("경기 부천시", response.regionName());
    }

    @Test
    void deletedIs404() {
        item.delete();

        assertEquals(ErrorCode.ITEM_NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.detail(DONOR_ID, 20L)).getErrorCode());
    }

    @Test
    void browseMapsCardsAndPages() {
        Organization organization = ItemFixtures.organization(ORGANIZATION_ID);
        List<NeededItem> fetched = List.of(ItemFixtures.neededItem(30L, organization), item);
        when(neededItemRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(fetched);

        var page = service.browse(DONOR_ID, new ItemSearchCondition(null, null, null, null), null, 1);

        assertTrue(page.hasNext());
        assertEquals(30L, page.nextCursor());
        NeededItemSummaryResponse card = page.items().getFirst();
        assertEquals("구로지역아동센터", card.organizationName());
        assertEquals("생활용품", card.categoryName());
        assertEquals(20, card.availableQuantity());
        assertEquals("https://example.com/needed-items/2/ref.jpg", card.referenceImageUrl());
    }

    @Test
    void mineRejectsBadSize() {
        assertEquals(ErrorCode.INVALID_INPUT, assertThrows(BusinessException.class,
                () -> service.mine(ORGANIZATION_ID, null, null, 51)).getErrorCode());
    }
}
