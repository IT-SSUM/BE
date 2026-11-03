package com.itssum.domain.item.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itssum.domain.item.ItemFixtures;
import com.itssum.domain.item.dto.request.CreateNeededItemRequest;
import com.itssum.domain.item.dto.request.UpdateNeededItemRequest;
import com.itssum.domain.item.dto.response.NeededItemDetailResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.NeededItemStatus;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.image.ItemImageKind;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.ItemCategoryRepository;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.item.repository.RegionRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.entity.RequesterType;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.domain.notification.entity.NotificationType;
import com.itssum.domain.notification.service.NotificationService;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class NeededItemCommandServiceTest {

    private static final Long ORGANIZATION_ID = 2L;
    private static final String REFERENCE_KEY = "needed-items/2/ref.jpg";

    private final MemberReader memberReader = mock(MemberReader.class);
    private final ItemCategoryRepository categoryRepository = mock(ItemCategoryRepository.class);
    private final RegionRepository regionRepository = mock(RegionRepository.class);
    private final ItemImages itemImages = mock(ItemImages.class);
    private final NeededItemRepository neededItemRepository = mock(NeededItemRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final NeededItemCommandService service = new NeededItemCommandService(memberReader,
            new ItemReferences(categoryRepository, regionRepository), itemImages, neededItemRepository, matchRepository,
            new TransactionTemplate(mock(PlatformTransactionManager.class)), notificationService);

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = ItemFixtures.organization(ORGANIZATION_ID);
        when(memberReader.activeOrganization(ORGANIZATION_ID)).thenReturn(organization);
        when(memberReader.lockActiveOrganization(ORGANIZATION_ID)).thenReturn(organization);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(ItemFixtures.category(1L, "생활용품")));
        when(regionRepository.findById(Region.PILOT_REGION_ID)).thenReturn(Optional.of(ItemFixtures.pilotRegion()));
        when(neededItemRepository.save(any(NeededItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemImages.readUrl(any())).thenAnswer(invocation ->
                invocation.getArgument(0) == null ? null : "https://example.com/" + invocation.getArgument(0));
    }

    @Test
    void createSavesWithoutGeocoding() {
        NeededItemDetailResponse response = service.create(ORGANIZATION_ID, request("병", REFERENCE_KEY));

        NeededItem saved = savedItem();
        assertEquals("생수", saved.getItemName());
        assertEquals(20, saved.getRequiredQuantity());
        assertEquals(ConditionGrade.HIGH, saved.getDesiredCondition());
        assertEquals(REFERENCE_KEY, saved.getReferenceImageUrl());
        assertNull(saved.getLatitude());
        assertNull(saved.getReceiveAddress());
        assertTrue(response.mine());
        assertEquals("https://example.com/" + REFERENCE_KEY, response.referenceImageUrl());
        assertEquals(REFERENCE_KEY, response.referenceImageKey());
        assertEquals("구로지역아동센터", response.organizationName());
        assertEquals("서울 구로구 가마산로 12", response.organizationAddress());
        assertEquals(20, response.availableQuantity());
    }

    @Test
    void referenceImageIsVerifiedWhenSent() {
        service.create(ORGANIZATION_ID, request("병", REFERENCE_KEY));

        verify(itemImages).verifyUploaded(ItemImageKind.NEEDED, ORGANIZATION_ID, List.of(REFERENCE_KEY));
    }

    @Test
    void withoutReferenceImageSkipsS3() {
        NeededItemDetailResponse response = service.create(ORGANIZATION_ID, request("병", null));

        verify(itemImages, never()).verifyUploaded(any(), any(), any());
        assertNull(savedItem().getReferenceImageUrl());
        assertNull(response.referenceImageUrl());
    }

    @Test
    void unsupportedUnitIs400() {
        assertEquals(ErrorCode.ITEM_UNIT_NOT_SUPPORTED, assertThrows(BusinessException.class,
                () -> service.create(ORGANIZATION_ID, request("ea", REFERENCE_KEY))).getErrorCode());
        verifyNoInteractions(itemImages);
        verify(neededItemRepository, never()).save(any());
    }

    @Test
    void foreignReferenceImageIs400() {
        String foreign = "needed-items/9/ref.jpg";
        doThrow(new BusinessException(ErrorCode.ITEM_IMAGE_NOT_UPLOADED))
                .when(itemImages).verifyUploaded(ItemImageKind.NEEDED, ORGANIZATION_ID, List.of(foreign));

        assertEquals(ErrorCode.ITEM_IMAGE_NOT_UPLOADED, assertThrows(BusinessException.class,
                () -> service.create(ORGANIZATION_ID, request("병", foreign))).getErrorCode());
        verify(neededItemRepository, never()).save(any());
    }

    @Test
    void updateChangesSentFields() {
        NeededItem item = existingItem();

        NeededItemDetailResponse response = service.update(ORGANIZATION_ID, 20L,
                new UpdateNeededItemRequest("평일 오후에 받아요", "경기 부천시 길주로 1", null));

        assertEquals("평일 오후에 받아요", item.getDescription());
        assertEquals("경기 부천시 길주로 1", item.getReceiveAddress());
        assertEquals(REFERENCE_KEY, item.getReferenceImageUrl());
        assertEquals("경기 부천시 길주로 1", response.receiveAddress());
        verify(itemImages, never()).verifyUploaded(any(), any(), any());
    }

    @Test
    void emptyValuesClear() {
        NeededItem item = existingItem();

        service.update(ORGANIZATION_ID, 20L, new UpdateNeededItemRequest("", " ", ""));

        assertNull(item.getDescription());
        assertNull(item.getReceiveAddress());
        assertNull(item.getReferenceImageUrl());
        verify(itemImages, never()).verifyUploaded(any(), any(), any());
    }

    @Test
    void sameReferenceImageKeySkipsS3() {
        NeededItem item = existingItem();

        service.update(ORGANIZATION_ID, 20L, new UpdateNeededItemRequest(null, null, REFERENCE_KEY));

        verify(itemImages, never()).verifyUploaded(any(), any(), any());
        assertEquals(REFERENCE_KEY, item.getReferenceImageUrl());
    }

    @Test
    void newReferenceImageKeyIsVerified() {
        NeededItem item = existingItem();
        String added = "needed-items/2/new.jpg";

        service.update(ORGANIZATION_ID, 20L, new UpdateNeededItemRequest(null, null, added));

        verify(itemImages).verifyUploaded(ItemImageKind.NEEDED, ORGANIZATION_ID, List.of(added));
        assertEquals(added, item.getReferenceImageUrl());
    }

    @Test
    void othersItemIs403() {
        existingItem();

        assertEquals(ErrorCode.ITEM_NOT_OWNER, assertThrows(BusinessException.class,
                () -> service.update(9L, 20L, new UpdateNeededItemRequest("x", null, null))).getErrorCode());
        assertEquals(ErrorCode.ITEM_NOT_OWNER,
                assertThrows(BusinessException.class, () -> service.delete(9L, 20L)).getErrorCode());
    }

    @Test
    void deletedItemIs404() {
        existingItem().delete();

        assertEquals(ErrorCode.ITEM_NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.update(ORGANIZATION_ID, 20L, new UpdateNeededItemRequest("x", null, null))).getErrorCode());
        assertEquals(ErrorCode.ITEM_NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.delete(ORGANIZATION_ID, 20L)).getErrorCode());
    }

    @Test
    void deleteBlockedWhilePendingMatch() {
        NeededItem item = existingItem();
        when(matchRepository.existsByNeededItemIdAndStatusNotIn(20L, MatchStatus.FINISHED)).thenReturn(true);

        assertEquals(ErrorCode.ITEM_DELETE_BLOCKED,
                assertThrows(BusinessException.class, () -> service.delete(ORGANIZATION_ID, 20L)).getErrorCode());
        assertFalse(item.isDeleted());
    }

    @Test
    void deleteSoftDeletesWithLock() {
        NeededItem item = existingItem();

        service.delete(ORGANIZATION_ID, 20L);

        assertTrue(item.isDeleted());
        verify(neededItemRepository).findByIdForUpdate(20L);
    }

    @Test
    void closeRejectsDonorProposalsAndWithdrawsOwnRequests() {
        NeededItem item = existingItem();
        Match proposal = pendingMatch(item, RequesterType.DONOR);
        Match request = pendingMatch(item, RequesterType.ORGANIZATION);
        when(matchRepository.findByNeededItemIdAndStatusForUpdate(20L, MatchStatus.PENDING))
                .thenReturn(List.of(proposal, request));

        NeededItemDetailResponse response = service.close(ORGANIZATION_ID, 20L);

        assertEquals(NeededItemStatus.CLOSED, response.status());
        assertEquals(NeededItemStatus.CLOSED, item.getStatus());
        assertEquals(MatchStatus.REJECTED, proposal.getStatus());
        assertEquals(MatchStatus.CANCELED, request.getStatus());
        assertEquals(organization.getUser(), request.getCanceledBy());
        verify(notificationService).createForMatch(proposal, organization.getUser(), NotificationType.MATCH_REJECTED);
        verify(notificationService, never()).createForMatch(eq(request), any(), any());
    }

    @Test
    void closeAlreadyClosedTouchesNothing() {
        existingItem().close();

        NeededItemDetailResponse response = service.close(ORGANIZATION_ID, 20L);

        assertEquals(NeededItemStatus.CLOSED, response.status());
        verify(matchRepository, never()).findByNeededItemIdAndStatusForUpdate(any(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void closeOthersItemIs403() {
        existingItem();

        assertEquals(ErrorCode.ITEM_NOT_OWNER,
                assertThrows(BusinessException.class, () -> service.close(3L, 20L)).getErrorCode());
    }

    @Test
    void closeDeletedItemIs404() {
        existingItem().delete();

        assertEquals(ErrorCode.ITEM_NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.close(ORGANIZATION_ID, 20L)).getErrorCode());
    }

    private static Match pendingMatch(NeededItem needed, RequesterType requestedBy) {
        Donor donor = ItemFixtures.donor(1L);
        return Match.builder().donationItem(ItemFixtures.donationItem(10L, donor, List.of("donation-items/1/a.jpg")))
                .neededItem(needed).donor(donor).organization(needed.getOrganization())
                .requestedBy(requestedBy).quantity(5).build();
    }

    private NeededItem existingItem() {
        NeededItem item = ItemFixtures.neededItem(20L, organization);
        when(neededItemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(neededItemRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(item));
        return item;
    }

    private NeededItem savedItem() {
        ArgumentCaptor<NeededItem> captor = ArgumentCaptor.forClass(NeededItem.class);
        verify(neededItemRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private static CreateNeededItemRequest request(String unit, String referenceImageKey) {
        return new CreateNeededItemRequest(1L, " 생수 ", 20, unit, null, ConditionGrade.HIGH,
                "아이들이 마실 생수가 필요해요", referenceImageKey);
    }
}
