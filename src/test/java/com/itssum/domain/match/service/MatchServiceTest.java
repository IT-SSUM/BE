package com.itssum.domain.match.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.repository.ChatRoomRepository;
import com.itssum.domain.chat.service.ChatRoomService;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.DonationItemStatus;
import com.itssum.domain.item.entity.ItemCategory;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.NeededItemStatus;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.match.dto.request.ConfirmAppointmentRequest;
import com.itssum.domain.match.dto.request.CreateMatchRequest;
import com.itssum.domain.match.entity.DeliveryMethod;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.entity.RequesterType;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MatchServiceTest {

    private final MatchRepository matches = mock(MatchRepository.class);
    private final DonationItemRepository donations = mock(DonationItemRepository.class);
    private final NeededItemRepository needs = mock(NeededItemRepository.class);
    private final ChatRoomService rooms = mock(ChatRoomService.class);
    private final ChatRoomRepository roomRepository = mock(ChatRoomRepository.class);
    private final ChatRoom chatRoom = mock(ChatRoom.class);
    private final MatchService service = new MatchService(matches, donations, needs, rooms, roomRepository);

    private DonationItem donation;
    private NeededItem needed;
    private Match match;
    private User donorUser;
    private User organizationUser;

    @BeforeEach
    void setUp() {
        Donor donor = mock(Donor.class);
        Organization organization = mock(Organization.class);
        when(donor.getId()).thenReturn(1L);
        when(donor.getName()).thenReturn("최윤서");
        when(organization.getId()).thenReturn(2L);
        when(organization.getName()).thenReturn("기관 A");
        when(organization.getAddress()).thenReturn("경기 부천시 길 12");
        ItemCategory category = mock(ItemCategory.class);
        when(category.getId()).thenReturn(3L);
        when(category.getName()).thenReturn("도서·문구");
        Region region = Region.builder().sido("경기").sigungu("부천시").build();
        donation = DonationItem.builder().donor(donor).category(category).region(region)
                .itemName("도서").quantity(5).unit("권").conditionGrade(ConditionGrade.HIGH).build();
        needed = NeededItem.builder().organization(organization).category(category).region(region)
                .itemName("도서").requiredQuantity(2).unit("권")
                .desiredCondition(ConditionGrade.MID).receiveAddress("기관 입구").build();
        ReflectionTestUtils.setField(donation, "id", 10L);
        ReflectionTestUtils.setField(needed, "id", 20L);
        match = Match.builder().donationItem(donation).neededItem(needed).donor(donor)
                .organization(organization).requestedBy(RequesterType.ORGANIZATION).quantity(2).build();
        ReflectionTestUtils.setField(match, "id", 7L);
        donorUser = mock(User.class);
        organizationUser = mock(User.class);
        when(donorUser.getId()).thenReturn(1L);
        when(organizationUser.getId()).thenReturn(2L);
        when(matches.findByIdForUpdate(7L)).thenReturn(Optional.of(match));
        when(chatRoom.getId()).thenReturn(33L);
        when(rooms.createForMatch(any(Match.class))).thenReturn(chatRoom);
        when(roomRepository.findByMatch_Id(7L)).thenReturn(Optional.of(chatRoom));
        when(donations.findByIdForUpdate(10L)).thenReturn(Optional.of(donation));
        when(needs.findByIdForUpdate(20L)).thenReturn(Optional.of(needed));
    }

    @Test
    void creationWaitsForRecipientWithoutOpeningChatOrReservingQuantity() {
        when(donorUser.getRole()).thenReturn(UserRole.DONOR);
        when(donations.findById(10L)).thenReturn(Optional.of(donation));
        when(needs.findById(20L)).thenReturn(Optional.of(needed));
        when(matches.save(any(Match.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.create(donorUser, new CreateMatchRequest(10L, 20L, 2, "도서를 기부합니다"));

        assertEquals(MatchStatus.PENDING, response.status());
        assertEquals(0, donation.getReservedQuantity());
        assertEquals(0, needed.getReservedQuantity());
        verify(rooms, never()).createForMatch(any(Match.class));
    }

    @Test
    void onlyRecipientCanAcceptAndAcceptanceOpensChatWithoutReserving() {
        assertEquals(ErrorCode.MATCH_NOT_RECIPIENT,
                assertThrows(BusinessException.class, () -> service.accept(7L, organizationUser)).getErrorCode());

        var response = service.accept(7L, donorUser);

        assertEquals(MatchStatus.TRADING, response.status());
        assertNotNull(response.respondedAt());
        assertNull(match.getReservedAt());
        assertEquals(33L, response.chatRoomId());
        assertEquals(0, donation.getReservedQuantity());
        assertEquals(0, needed.getReservedQuantity());
        verify(rooms).createForMatch(match);
        assertEquals(ErrorCode.MATCH_INVALID_STATUS,
                assertThrows(BusinessException.class, () -> service.accept(7L, donorUser)).getErrorCode());
    }

    @Test
    void deletedItemCannotBeAccepted() {
        donation.delete();

        BusinessException error = assertThrows(BusinessException.class, () -> service.accept(7L, donorUser));

        assertEquals(ErrorCode.MATCH_INVALID_ITEM, error.getErrorCode());
        assertEquals(MatchStatus.PENDING, match.getStatus());
        verify(rooms, never()).createForMatch(any(Match.class));
    }

    @Test
    void organizationAcceptsDonorProposal() {
        Match proposal = Match.builder().donationItem(donation).neededItem(needed)
                .donor(match.getDonor()).organization(match.getOrganization())
                .requestedBy(RequesterType.DONOR).quantity(2).build();
        ReflectionTestUtils.setField(proposal, "id", 8L);
        when(matches.findByIdForUpdate(8L)).thenReturn(Optional.of(proposal));

        assertEquals(ErrorCode.MATCH_NOT_RECIPIENT,
                assertThrows(BusinessException.class, () -> service.accept(8L, donorUser)).getErrorCode());
        assertEquals(MatchStatus.TRADING, service.accept(8L, organizationUser).status());
        verify(rooms).createForMatch(proposal);
    }

    @Test
    void recipientCanRejectWithoutOpeningChat() {
        assertEquals(ErrorCode.MATCH_NOT_RECIPIENT,
                assertThrows(BusinessException.class, () -> service.reject(7L, organizationUser)).getErrorCode());

        var response = service.reject(7L, donorUser);

        assertEquals(MatchStatus.REJECTED, response.status());
        assertEquals(0, donation.getReservedQuantity());
        verify(rooms, never()).createForMatch(any(Match.class));
        assertEquals(ErrorCode.MATCH_INVALID_STATUS,
                assertThrows(BusinessException.class, () -> service.reserve(7L, donorUser)).getErrorCode());
    }

    @Test
    void onlyDonorReservesAndCanceledReservationRestoresBothQuantities() {
        service.accept(7L, donorUser);
        BusinessException forbidden = assertThrows(BusinessException.class, () -> service.reserve(7L, organizationUser));
        assertEquals(ErrorCode.MATCH_NOT_PARTICIPANT, forbidden.getErrorCode());

        service.reserve(7L, donorUser);
        assertEquals(MatchStatus.RESERVED, match.getStatus());
        assertNotNull(match.getReservedAt());
        assertEquals(2, donation.getReservedQuantity());
        assertEquals(2, needed.getReservedQuantity());
        assertEquals(NeededItemStatus.FULL, needed.getStatus());
        BusinessException duplicate = assertThrows(BusinessException.class, () -> service.reserve(7L, donorUser));
        assertEquals(ErrorCode.MATCH_INVALID_STATUS, duplicate.getErrorCode());

        service.cancel(7L, organizationUser, "거래 취소");
        assertEquals(MatchStatus.CANCELED, match.getStatus());
        assertEquals(0, donation.getReservedQuantity());
        assertEquals(0, needed.getReservedQuantity());
        assertEquals(NeededItemStatus.RECRUITING, needed.getStatus());
    }

    @Test
    void organizationReceiptMovesReservedQuantityToCompletedAndClosesNeed() {
        service.accept(7L, donorUser);
        service.reserve(7L, donorUser);
        service.complete(7L, organizationUser);

        assertEquals(MatchStatus.COMPLETED, match.getStatus());
        assertEquals(0, donation.getReservedQuantity());
        assertEquals(2, donation.getCompletedQuantity());
        assertEquals(DonationItemStatus.RECRUITING, donation.getStatus());
        assertEquals(0, needed.getReservedQuantity());
        assertEquals(2, needed.getReceivedQuantity());
        assertEquals(NeededItemStatus.CLOSED, needed.getStatus());
        assertEquals(ErrorCode.MATCH_INVALID_STATUS,
                assertThrows(BusinessException.class, () -> service.cancel(7L, donorUser, null)).getErrorCode());
    }

    @Test
    void eitherParticipantCanReviseAppointmentWhileReserved() {
        service.accept(7L, donorUser);
        ConfirmAppointmentRequest request = new ConfirmAppointmentRequest(DeliveryMethod.DIRECT,
                OffsetDateTime.now().plusDays(2), "기관 입구", "담당자");
        assertEquals(ErrorCode.MATCH_INVALID_STATUS,
                assertThrows(BusinessException.class,
                        () -> service.confirmAppointment(7L, organizationUser, request)).getErrorCode());
        service.reserve(7L, donorUser);

        var response = service.confirmAppointment(7L, organizationUser, request);

        assertEquals("기관 입구", response.deliveryPlace());
        assertEquals("최윤서", response.donorName());
        assertEquals("기관 A", response.organizationName());
        assertEquals(33L, response.chatRoomId());
        assertEquals("경기 부천시", response.donationItemRegion());
        assertEquals(3, response.donationItemAvailableQuantity());
        verify(rooms).createAppointmentCard(match, organizationUser);
        ConfirmAppointmentRequest revision = new ConfirmAppointmentRequest(DeliveryMethod.DIRECT,
                OffsetDateTime.now().plusDays(3), "변경된 장소", "담당자");
        assertEquals("변경된 장소", service.confirmAppointment(7L, donorUser, revision).deliveryPlace());
        verify(rooms).createAppointmentCard(match, donorUser);
    }
}
