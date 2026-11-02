package com.itssum.domain.item.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itssum.domain.item.ItemFixtures;
import com.itssum.domain.item.dto.request.CreateDonationItemRequest;
import com.itssum.domain.item.dto.response.DonationItemDetailResponse;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.image.ItemImageKind;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.item.repository.ItemCategoryRepository;
import com.itssum.domain.item.repository.RegionRepository;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.service.AddressGeocoder;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.kakao.GeoPoint;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class DonationItemCommandServiceTest {

    private static final Long DONOR_ID = 1L;
    private static final GeoPoint POINT = new GeoPoint(new BigDecimal("37.5034138"), new BigDecimal("126.7660309"));
    private static final List<String> KEYS = List.of("donation-items/1/a.jpg", "donation-items/1/b.jpg");

    private final MemberReader memberReader = mock(MemberReader.class);
    private final ItemCategoryRepository categoryRepository = mock(ItemCategoryRepository.class);
    private final RegionRepository regionRepository = mock(RegionRepository.class);
    private final ItemImages itemImages = mock(ItemImages.class);
    private final AddressGeocoder addressGeocoder = mock(AddressGeocoder.class);
    private final DonationItemRepository donationItemRepository = mock(DonationItemRepository.class);
    private final DonationItemCommandService service = new DonationItemCommandService(memberReader,
            new ItemReferences(categoryRepository, regionRepository), itemImages, addressGeocoder,
            donationItemRepository, new TransactionTemplate(mock(PlatformTransactionManager.class)));

    private Donor donor;

    @BeforeEach
    void setUp() {
        donor = ItemFixtures.donor(DONOR_ID);
        when(memberReader.activeDonor(DONOR_ID)).thenReturn(donor);
        when(memberReader.lockActiveDonor(DONOR_ID)).thenReturn(donor);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(ItemFixtures.category(1L, "생활용품")));
        when(regionRepository.findById(Region.PILOT_REGION_ID)).thenReturn(Optional.of(ItemFixtures.pilotRegion()));
        when(donationItemRepository.save(any(DonationItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemImages.readUrl(any())).thenAnswer(invocation -> "https://example.com/" + invocation.getArgument(0));
    }

    @Test
    void createSavesItemWithImagesAndCoordinates() {
        when(addressGeocoder.locate(ItemFixtures.PICKUP_ADDRESS)).thenReturn(Optional.of(POINT));

        DonationItemDetailResponse response = service.create(DONOR_ID, request("장", KEYS));

        DonationItem saved = savedItem();
        assertEquals(POINT.latitude(), saved.getLatitude());
        assertEquals(POINT.longitude(), saved.getLongitude());
        assertEquals(KEYS, saved.imageKeys());
        assertEquals(Region.PILOT_REGION_ID, saved.getRegion().getId());
        assertEquals(ItemFixtures.PICKUP_ADDRESS, saved.getPickupAddress());
        assertEquals("2층", saved.getPickupAddressDetail());
        assertEquals("수건", saved.getItemName());
        verify(itemImages).verifyUploaded(ItemImageKind.DONATION, DONOR_ID, KEYS);
        assertTrue(response.mine());
        assertEquals(List.of("https://example.com/donation-items/1/a.jpg", "https://example.com/donation-items/1/b.jpg"),
                response.imageUrls());
        assertEquals(KEYS, response.imageKeys());
        assertEquals("경기 부천시", response.regionName());
        assertEquals(30, response.availableQuantity());
    }

    @Test
    void createWithoutCoordinatesWhenGeocodingFails() {
        when(addressGeocoder.locate(ItemFixtures.PICKUP_ADDRESS)).thenReturn(Optional.empty());

        service.create(DONOR_ID, request("장", KEYS));

        assertNull(savedItem().getLatitude());
        assertNull(savedItem().getLongitude());
    }

    @Test
    void unsupportedUnitIsRejectedBeforeS3() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(DONOR_ID, request("책", KEYS)));

        assertEquals(ErrorCode.ITEM_UNIT_NOT_SUPPORTED, error.getErrorCode());
        verifyNoInteractions(itemImages, addressGeocoder);
        verify(donationItemRepository, never()).save(any());
    }

    @Test
    void foreignImageKeyIsRejected() {
        List<String> foreign = List.of("donation-items/99/a.jpg");
        doThrow(new BusinessException(ErrorCode.ITEM_IMAGE_NOT_UPLOADED))
                .when(itemImages).verifyUploaded(ItemImageKind.DONATION, DONOR_ID, foreign);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(DONOR_ID, request("장", foreign)));

        assertEquals(ErrorCode.ITEM_IMAGE_NOT_UPLOADED, error.getErrorCode());
        verify(donationItemRepository, never()).save(any());
        verifyNoInteractions(addressGeocoder);
    }

    @Test
    void duplicateImageKeysAre400() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(DONOR_ID, request("장", List.of("donation-items/1/a.jpg", "donation-items/1/a.jpg"))));

        assertEquals(ErrorCode.INVALID_INPUT, error.getErrorCode());
        verifyNoInteractions(itemImages, addressGeocoder);
    }

    @Test
    void withdrawnDonorIs401() {
        when(memberReader.activeDonor(DONOR_ID)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        assertEquals(ErrorCode.UNAUTHORIZED, assertThrows(BusinessException.class,
                () -> service.create(DONOR_ID, request("장", KEYS))).getErrorCode());
        verifyNoInteractions(itemImages, addressGeocoder);
    }

    private DonationItem savedItem() {
        ArgumentCaptor<DonationItem> captor = ArgumentCaptor.forClass(DonationItem.class);
        verify(donationItemRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private static CreateDonationItemRequest request(String unit, List<String> imageKeys) {
        return new CreateDonationItemRequest(1L, " 수건 ", 30, unit, ConditionGrade.MID, "깨끗한 수건이에요", imageKeys,
                " " + ItemFixtures.PICKUP_ADDRESS + " ", "2층", null);
    }
}
