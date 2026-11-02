package com.itssum.domain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.match.entity.DeliveryMethod;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.RequesterType;
import com.itssum.domain.notification.entity.Notification;
import com.itssum.domain.notification.entity.NotificationTargetType;
import com.itssum.domain.notification.entity.NotificationType;
import com.itssum.domain.notification.event.NotificationCreatedEvent;
import com.itssum.domain.notification.repository.NotificationRepository;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

class NotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final NotificationService service = new NotificationService(repository, events);

    @Test
    void matchChangeNotifiesOnlyTheOtherParticipant() {
        User donorUser = user(1L);
        User organizationUser = user(2L);
        Donor donor = mock(Donor.class);
        Organization organization = mock(Organization.class);
        DonationItem item = mock(DonationItem.class);
        Match match = mock(Match.class);
        when(donor.getUser()).thenReturn(donorUser);
        when(organization.getUser()).thenReturn(organizationUser);
        when(match.getDonor()).thenReturn(donor);
        when(match.getOrganization()).thenReturn(organization);
        when(match.getDonationItem()).thenReturn(item);
        when(match.getId()).thenReturn(7L);
        when(match.getQuantity()).thenReturn(3);
        when(item.getItemName()).thenReturn("도서");
        when(item.getUnit()).thenReturn("권");
        when(repository.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));

        service.createForMatch(match, donorUser, NotificationType.MATCH_RESERVED);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertEquals(2L, saved.getValue().getReceiver().getId());
        assertEquals(NotificationType.MATCH_RESERVED, saved.getValue().getType());
        assertEquals(NotificationTargetType.MATCH, saved.getValue().getTargetType());
        assertEquals(7L, saved.getValue().getTargetId());
        ArgumentCaptor<NotificationCreatedEvent> published = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(events).publishEvent(published.capture());
        assertEquals(2L, published.getValue().receiverId());
        assertEquals("도서 3권이 예약됐어요. 전달 일정을 협의해 주세요.",
                published.getValue().notification().content());

        assertEquals(ErrorCode.MATCH_NOT_PARTICIPANT, assertThrows(BusinessException.class,
                () -> service.createForMatch(match, user(3L), NotificationType.MATCH_RESERVED)).getErrorCode());
    }

    @Test
    void matchAlertsExplainActionsAndReservedCancellationOnly() {
        User donorUser = user(1L);
        User organizationUser = user(2L);
        Donor donor = mock(Donor.class);
        Organization organization = mock(Organization.class);
        DonationItem item = mock(DonationItem.class);
        Match match = mock(Match.class);
        when(donor.getUser()).thenReturn(donorUser);
        when(donor.getName()).thenReturn("최윤서");
        when(organization.getUser()).thenReturn(organizationUser);
        when(organization.getName()).thenReturn("기관 B");
        when(match.getDonor()).thenReturn(donor);
        when(match.getOrganization()).thenReturn(organization);
        when(match.getDonationItem()).thenReturn(item);
        when(match.getRequestedBy()).thenReturn(RequesterType.DONOR);
        when(match.getQuantity()).thenReturn(10);
        when(item.getItemName()).thenReturn("생수");
        when(item.getUnit()).thenReturn("개");
        when(repository.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));

        service.createForMatch(match, donorUser, NotificationType.MATCH_REQUESTED);
        service.createForMatch(match, organizationUser, NotificationType.MATCH_ACCEPTED);
        service.createForMatch(match, organizationUser, NotificationType.MATCH_CANCELED);
        when(match.getReservedAt()).thenReturn(LocalDateTime.of(2026, 11, 1, 12, 0));
        service.createForMatch(match, organizationUser, NotificationType.MATCH_CANCELED);
        when(match.getScheduledAt()).thenReturn(LocalDateTime.of(2026, 11, 7, 14, 0));
        when(match.getDeliveryMethod()).thenReturn(DeliveryMethod.DIRECT);
        service.createForMatch(match, donorUser, NotificationType.MATCH_SCHEDULED);
        service.createForMatch(match, donorUser, NotificationType.MATCH_RECEIVE_REQUIRED);

        ArgumentCaptor<Notification> alerts = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(6)).save(alerts.capture());
        assertEquals("새로운 기부 제안", alerts.getAllValues().get(0).getTitle());
        assertEquals("최윤서 님이 생수 10개를 제안했어요.", alerts.getAllValues().get(0).getContent());
        assertEquals("기관 B가 생수 10개 기부 제안을 수락했어요. 거래중이며 수량은 아직 확보되지 않았어요.",
                alerts.getAllValues().get(1).getContent());
        assertEquals("거래 취소", alerts.getAllValues().get(2).getTitle());
        assertEquals("거래 취소 · 예약 수량 반환", alerts.getAllValues().get(3).getTitle());
        assertEquals("전달 일정이 정해졌어요", alerts.getAllValues().get(4).getTitle());
        assertEquals("수령 여부를 확인해 주세요", alerts.getAllValues().get(5).getTitle());
        assertEquals("생수 10개를 받았다면 확인해 주세요.", alerts.getAllValues().get(5).getContent());
    }

    @Test
    void onlyOwnerCanReadNotificationAndRepeatReadKeepsTimestamp() {
        User owner = user(2L);
        Notification notification = Notification.builder().receiver(owner).type(NotificationType.MATCH_ACCEPTED)
                .title("수락").targetType(NotificationTargetType.MATCH).targetId(7L).build();
        when(repository.findById(9L)).thenReturn(Optional.of(notification));

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.markRead(9L, 1L)).getErrorCode());
        assertFalse(notification.isRead());
        service.markRead(9L, 2L);
        assertTrue(notification.isRead());
        assertNotNull(notification.getReadAt());
        var firstReadAt = notification.getReadAt();
        service.markRead(9L, 2L);
        assertEquals(firstReadAt, notification.getReadAt());
    }

    @Test
    void listUsesCursorAndUnreadCountUsesOwner() {
        Notification newer = mock(Notification.class);
        Notification older = mock(Notification.class);
        when(newer.getId()).thenReturn(12L);
        when(older.getId()).thenReturn(11L);
        when(repository.findByReceiver_IdOrderByIdDesc(eq(2L), any(Pageable.class)))
                .thenReturn(List.of(newer, older));
        when(repository.countByReceiver_IdAndIsReadFalse(2L)).thenReturn(4L);

        var page = service.list(2L, null, 1);

        assertEquals(1, page.notifications().size());
        assertEquals(12L, page.nextCursor());
        assertTrue(page.hasNext());
        assertEquals(4L, service.unreadCount(2L).unreadCount());
        assertEquals(ErrorCode.INVALID_INPUT, assertThrows(BusinessException.class,
                () -> service.list(2L, null, 101)).getErrorCode());
    }

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
