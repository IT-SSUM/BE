package com.itssum.domain.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.domain.item.ItemFixtures;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.recommendation.dto.response.OrganizationRecommendationResponse;
import com.itssum.domain.recommendation.entity.OrganizationRecommendation;
import com.itssum.domain.recommendation.repository.OrganizationRecommendationRepository;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

class RecommendationServiceTest {

    private static final Long DONOR_ID = 1L;
    private static final Long OTHER_DONOR_ID = 2L;
    private static final Long ITEM_ID = 10L;
    private static final BigDecimal LAT = new BigDecimal("37.5034138");
    private static final BigDecimal LNG = new BigDecimal("126.7660309");

    private final MemberReader memberReader = mock(MemberReader.class);
    private final DonationItemRepository donationItemRepository = mock(DonationItemRepository.class);
    private final NeededItemRepository neededItemRepository = mock(NeededItemRepository.class);
    private final OrganizationRecommendationRepository recommendationRepository =
            mock(OrganizationRecommendationRepository.class);
    private final RecommendationService service = new RecommendationService(memberReader, donationItemRepository,
            neededItemRepository, recommendationRepository);

    private DonationItem item;

    @BeforeEach
    void setUp() {
        item = ItemFixtures.donationItem(ITEM_ID, ItemFixtures.donor(DONOR_ID), List.of("donation-items/1/a.jpg"));
        when(donationItemRepository.findByIdForUpdate(ITEM_ID)).thenReturn(Optional.of(item));
        when(donationItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(recommendationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void recommendRejectsOthersItem() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.recommend(OTHER_DONOR_ID, ITEM_ID));

        assertEquals(ErrorCode.RECOMMENDATION_NOT_OWNER, e.getErrorCode());
        verify(recommendationRepository, never()).deleteAllByDonationItemId(anyLong());
        verify(recommendationRepository, never()).saveAll(anyList());
    }

    @Test
    void recommendDeletedItemIs404() {
        item.delete();

        BusinessException e = assertThrows(BusinessException.class, () -> service.recommend(DONOR_ID, ITEM_ID));

        assertEquals(ErrorCode.ITEM_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void recommendFullItemClearsAndReturnsEmpty() {
        item.reserve(30);

        List<OrganizationRecommendationResponse> response = service.recommend(DONOR_ID, ITEM_ID);

        assertTrue(response.isEmpty());
        verify(recommendationRepository).deleteAllByDonationItemId(ITEM_ID);
        verify(neededItemRepository, never())
                .findRecommendationCandidates(anyLong(), anyLong(), anyString(), anyCollection(), anyCollection());
    }

    @Test
    void recommendQueriesMatchingRulesAndSavesRanked() {
        locate(item);
        NeededItem full = candidate(1L, 101L, 30);
        NeededItem half = candidate(2L, 102L, 15);
        when(neededItemRepository.findRecommendationCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(half, full));

        List<OrganizationRecommendationResponse> response = service.recommend(DONOR_ID, ITEM_ID);

        verify(neededItemRepository).findRecommendationCandidates(ITEM_ID, 1L, "장",
                EnumSet.of(ConditionGrade.MID, ConditionGrade.LOW), MatchStatus.FINISHED);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrganizationRecommendation>> saved = ArgumentCaptor.forClass(List.class);
        verify(recommendationRepository).saveAll(saved.capture());
        assertEquals(List.of(1, 2), saved.getValue().stream().map(OrganizationRecommendation::getRankNo).toList());
        assertEquals(List.of(full, half),
                saved.getValue().stream().map(OrganizationRecommendation::getNeededItem).toList());
        assertEquals("요청한 물품 분류와 상태에 맞아요. 필요 수량 30장 전체를 지원할 수 있어요. 기관과 매우 가까운 위치예요.",
                saved.getValue().get(0).getReason());
        assertEquals(101L, saved.getValue().get(0).getOrganization().getId());

        OrganizationRecommendationResponse first = response.get(0);
        assertEquals(1, first.rank());
        assertEquals(101L, first.organizationId());
        assertEquals("서울 구로구 가마산로 12", first.organizationAddress());
        assertEquals(1L, first.neededItemId());
        assertEquals(30, first.remainingQuantity());
        assertEquals("장", first.unit());
        assertEquals(ConditionGrade.MID, first.desiredCondition());
        assertEquals(new BigDecimal("100.00"), first.score());
        assertEquals(new BigDecimal("0.00"), first.distanceKm());
        assertEquals(saved.getValue().get(0).getReason(), first.reason());
        assertEquals(new BigDecimal("70.00"), response.get(1).score());
    }

    @Test
    void recommendDeletesBeforeSaving() {
        when(neededItemRepository.findRecommendationCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(candidate(1L, 101L, 30)));

        service.recommend(DONOR_ID, ITEM_ID);

        InOrder order = inOrder(recommendationRepository);
        order.verify(recommendationRepository).deleteAllByDonationItemId(ITEM_ID);
        order.verify(recommendationRepository).saveAll(anyList());
    }

    @Test
    void recommendWithoutDonationCoordinatesHasNullDistance() {
        NeededItem needed = candidate(1L, 101L, 30);
        ReflectionTestUtils.setField(needed, "unit", "병");
        when(neededItemRepository.findRecommendationCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(needed));

        OrganizationRecommendationResponse response = service.recommend(DONOR_ID, ITEM_ID).get(0);

        assertNull(response.distanceKm());
        assertEquals(new BigDecimal("60.00"), response.score());
        assertEquals("요청한 물품 분류와 상태에 맞아요. 필요 수량 30병 전체를 지원할 수 있어요.",
                response.reason());
    }

    @Test
    void findRenumbersAvailableRows() {
        when(recommendationRepository.findAvailable(ITEM_ID, MatchStatus.FINISHED)).thenReturn(List.of(
                saved(candidate(1L, 101L, 30), 1), saved(candidate(3L, 103L, 30), 3)));

        List<OrganizationRecommendationResponse> response = service.find(DONOR_ID, ITEM_ID);

        assertEquals(List.of(1, 2), response.stream().map(OrganizationRecommendationResponse::rank).toList());
        assertEquals(List.of(1L, 3L), response.stream().map(OrganizationRecommendationResponse::neededItemId).toList());
    }

    @Test
    void findFullItemReturnsEmptyWithoutQuery() {
        item.reserve(30);

        assertTrue(service.find(DONOR_ID, ITEM_ID).isEmpty());
        verify(recommendationRepository, never()).findAvailable(anyLong(), anyCollection());
    }

    @Test
    void findRejectsOthersItem() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.find(OTHER_DONOR_ID, ITEM_ID));

        assertEquals(ErrorCode.RECOMMENDATION_NOT_OWNER, e.getErrorCode());
    }

    @Test
    void findDeletedItemIs404() {
        item.delete();

        BusinessException e = assertThrows(BusinessException.class, () -> service.find(DONOR_ID, ITEM_ID));

        assertEquals(ErrorCode.ITEM_NOT_FOUND, e.getErrorCode());
    }

    private static NeededItem candidate(Long id, Long organizationId, int remaining) {
        Organization organization = ItemFixtures.organization(organizationId);
        ReflectionTestUtils.setField(organization, "latitude", LAT);
        ReflectionTestUtils.setField(organization, "longitude", LNG);
        NeededItem needed = ItemFixtures.neededItem(id, organization);
        ReflectionTestUtils.setField(needed, "requiredQuantity", remaining);
        ReflectionTestUtils.setField(needed, "unit", "장");
        ReflectionTestUtils.setField(needed, "desiredCondition", ConditionGrade.MID);
        return needed;
    }

    private static void locate(DonationItem donation) {
        ReflectionTestUtils.setField(donation, "latitude", LAT);
        ReflectionTestUtils.setField(donation, "longitude", LNG);
    }

    private OrganizationRecommendation saved(NeededItem needed, int rankNo) {
        return OrganizationRecommendation.builder().donationItem(item).neededItem(needed)
                .organization(needed.getOrganization()).rankNo(rankNo).score(new BigDecimal("60.00")).build();
    }
}
