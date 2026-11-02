package com.itssum.domain.item.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itssum.domain.item.ItemFixtures;
import com.itssum.domain.item.dto.request.CreateNeededItemRequest;
import com.itssum.domain.item.dto.response.NeededItemDetailResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.image.ItemImageKind;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.ItemCategoryRepository;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.item.repository.RegionRepository;
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
    private final NeededItemCommandService service = new NeededItemCommandService(memberReader,
            new ItemReferences(categoryRepository, regionRepository), itemImages, neededItemRepository,
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

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
