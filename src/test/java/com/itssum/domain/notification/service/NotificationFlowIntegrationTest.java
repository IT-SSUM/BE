package com.itssum.domain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.notification.entity.NotificationType;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class NotificationFlowIntegrationTest {

    @Autowired
    private NotificationService notifications;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long donorId;
    private Long organizationId;

    @AfterEach
    void cleanUp() {
        if (organizationId != null) {
            jdbcTemplate.update("delete from notifications where receiver_id = ?", organizationId);
        }
        if (donorId != null) {
            jdbcTemplate.update("delete from notifications where receiver_id = ?", donorId);
        }
        if (donorId != null && organizationId != null) {
            jdbcTemplate.update("delete from users where user_id in (?, ?)", donorId, organizationId);
        }
    }

    @Test
    void matchAlertPersistsAndOnlyReceiverCanReadIt() {
        String suffix = UUID.randomUUID().toString();
        User donor = users.save(User.builder().email("notification-donor-" + suffix + "@example.com")
                .password("test").role(UserRole.DONOR).build());
        donorId = donor.getId();
        User organization = users.save(User.builder().email("notification-org-" + suffix + "@example.com")
                .password("test").role(UserRole.ORGANIZATION).build());
        organizationId = organization.getId();
        Match match = match(donor, organization);

        transactionTemplate.executeWithoutResult(status ->
                notifications.createForMatch(match, donor, NotificationType.MATCH_RESERVED));

        var page = notifications.list(organizationId, null, 30);
        assertEquals(1, page.notifications().size());
        var alert = page.notifications().getFirst();
        assertNotNull(alert.notificationId());
        assertNotNull(alert.createdAt());
        assertEquals(100L, alert.targetId());
        assertEquals(1, notifications.unreadCount(organizationId).unreadCount());
        assertEquals(0, notifications.unreadCount(donorId).unreadCount());
        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, assertThrows(BusinessException.class,
                () -> notifications.markRead(alert.notificationId(), donorId)).getErrorCode());

        notifications.markRead(alert.notificationId(), organizationId);
        assertEquals(0, notifications.unreadCount(organizationId).unreadCount());
        transactionTemplate.executeWithoutResult(status ->
                notifications.createForMatch(match, donor, NotificationType.MATCH_SCHEDULED));
        assertEquals(1, notifications.unreadCount(organizationId).unreadCount());
        notifications.markAllRead(organizationId);
        assertEquals(0, notifications.unreadCount(organizationId).unreadCount());
        transactionTemplate.executeWithoutResult(status -> {
            notifications.createForMatch(match, donor, NotificationType.MATCH_CANCELED);
            status.setRollbackOnly();
        });
        assertEquals(2, notifications.list(organizationId, null, 30).notifications().size());
    }

    private Match match(User donorUser, User organizationUser) {
        Donor donor = mock(Donor.class);
        Organization organization = mock(Organization.class);
        DonationItem item = mock(DonationItem.class);
        Match match = mock(Match.class);
        when(donor.getUser()).thenReturn(donorUser);
        when(organization.getUser()).thenReturn(organizationUser);
        when(match.getDonor()).thenReturn(donor);
        when(match.getOrganization()).thenReturn(organization);
        when(match.getDonationItem()).thenReturn(item);
        when(match.getId()).thenReturn(100L);
        when(match.getQuantity()).thenReturn(3);
        when(item.getItemName()).thenReturn("도서");
        when(item.getUnit()).thenReturn("권");
        return match;
    }
}
