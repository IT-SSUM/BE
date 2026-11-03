package com.itssum.domain.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.entity.MessageType;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.notification.entity.NotificationType;
import com.itssum.domain.notification.service.NotificationService;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.infra.fcm.PushDeliveryResult;
import com.itssum.global.infra.fcm.PushSender;
import com.itssum.global.security.AccessTokenProvider;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationPushIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private AccessTokenProvider accessTokens;
    @Autowired private NotificationService notifications;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private PushSender pushSender;

    private Long donorId;
    private Long organizationId;

    @AfterEach
    void cleanUp() {
        for (Long id : new Long[]{donorId, organizationId}) {
            if (id == null) {
                continue;
            }
            jdbc.update("delete from notifications where receiver_id = ?", id);
            jdbc.update("delete from push_registrations where user_id = ?", id);
            jdbc.update("delete from notification_settings where user_id = ?", id);
            jdbc.update("delete from users where user_id = ?", id);
        }
    }

    @Test
    void settingsDisableOnlyPushWhileNotificationsRemainAndTokenMovesToNewAccount() throws Exception {
        User donor = user(UserRole.DONOR);
        donorId = donor.getId();
        User organization = user(UserRole.ORGANIZATION);
        organizationId = organization.getId();
        String donorAuth = auth(donor);
        String organizationAuth = auth(organization);

        mvc.perform(get("/api/users/me/notification-settings").header(HttpHeaders.AUTHORIZATION, organizationAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchEnabled").value(true))
                .andExpect(jsonPath("$.data.chatEnabled").value(true));
        mvc.perform(patch("/api/users/me/notification-settings")
                        .header(HttpHeaders.AUTHORIZATION, organizationAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchEnabled").value(false))
                .andExpect(jsonPath("$.data.chatEnabled").value(true));
        String fid = "fcm-test-" + UUID.randomUUID();
        Long registrationId = registrationId(register(organizationAuth, fid));

        transactions.executeWithoutResult(status ->
                notifications.createForMatch(match(donor, organization), donor, NotificationType.MATCH_RESERVED));
        assertEquals(1, notifications.unreadCount(organizationId).unreadCount());
        verify(pushSender, never()).send(eq(fid), any());

        mvc.perform(patch("/api/users/me/notification-settings")
                        .header(HttpHeaders.AUTHORIZATION, organizationAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchEnabled\":true}"))
                .andExpect(status().isOk());
        when(pushSender.send(eq(fid), any())).thenReturn(PushDeliveryResult.SENT);
        transactions.executeWithoutResult(status ->
                notifications.createForMatch(match(donor, organization), donor, NotificationType.MATCH_RESERVED));
        verify(pushSender).send(eq(fid), any());

        mvc.perform(patch("/api/users/me/notification-settings")
                        .header(HttpHeaders.AUTHORIZATION, organizationAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatEnabled\":false}"))
                .andExpect(status().isOk());
        ChatRoom room = org.mockito.Mockito.mock(ChatRoom.class);
        Match chatMatch = match(donor, organization);
        when(room.getId()).thenReturn(55L);
        when(room.getMatch()).thenReturn(chatMatch);
        transactions.executeWithoutResult(status ->
                notifications.createForChat(room, donor, MessageType.TEXT, "안녕하세요", 101L));
        assertEquals(3, notifications.unreadCount(organizationId).unreadCount());
        verifyNoMoreInteractions(pushSender);

        assertEquals(registrationId, registrationId(register(donorAuth, fid)));
        mvc.perform(delete("/api/users/me/push-registrations/{id}", registrationId)
                        .header(HttpHeaders.AUTHORIZATION, organizationAuth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_404_1"));
        mvc.perform(delete("/api/users/me/push-registrations/{id}", registrationId)
                        .header(HttpHeaders.AUTHORIZATION, donorAuth))
                .andExpect(status().isOk());
    }

    @Test
    void unregisteredFcmTokenIsRemovedAfterCommittedNotification() throws Exception {
        User donor = user(UserRole.DONOR);
        donorId = donor.getId();
        User organization = user(UserRole.ORGANIZATION);
        organizationId = organization.getId();
        String fid = "expired-" + UUID.randomUUID();
        register(auth(organization), fid);
        when(pushSender.send(eq(fid), any())).thenReturn(PushDeliveryResult.INVALID_REGISTRATION);

        transactions.executeWithoutResult(status ->
                notifications.createForMatch(match(donor, organization), donor, NotificationType.MATCH_RESERVED));

        assertEquals(0, jdbc.queryForObject(
                "select count(*) from push_registrations where fid = ?", Integer.class, fid));
        assertEquals(1, notifications.unreadCount(organizationId).unreadCount());
    }

    @Test
    void pushFailureDoesNotUndoSavedNotification() throws Exception {
        User donor = user(UserRole.DONOR);
        donorId = donor.getId();
        User organization = user(UserRole.ORGANIZATION);
        organizationId = organization.getId();
        String fid = "unavailable-" + UUID.randomUUID();
        register(auth(organization), fid);
        when(pushSender.send(eq(fid), any())).thenThrow(new IllegalStateException("FCM unavailable"));

        transactions.executeWithoutResult(status ->
                notifications.createForMatch(match(donor, organization), donor, NotificationType.MATCH_RESERVED));

        assertEquals(1, notifications.unreadCount(organizationId).unreadCount());
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from push_registrations where fid = ?", Integer.class, fid));
    }

    private User user(UserRole role) {
        return users.saveAndFlush(User.builder()
                .email("push-" + UUID.randomUUID() + "@example.com")
                .password("test").role(role).build());
    }

    private String auth(User user) {
        return "Bearer " + accessTokens.issue(user.getId(), user.getRole());
    }

    private ResultActions register(String authorization, String fid) throws Exception {
        return mvc.perform(post("/api/users/me/push-registrations")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fid\":\"%s\",\"deviceType\":\"ANDROID\"}".formatted(fid)))
                .andExpect(status().isCreated());
    }

    private Long registrationId(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(),
                "$.data.pushRegistrationId")).longValue();
    }

    private Match match(User donorUser, User organizationUser) {
        Donor donor = org.mockito.Mockito.mock(Donor.class);
        Organization organization = org.mockito.Mockito.mock(Organization.class);
        DonationItem item = org.mockito.Mockito.mock(DonationItem.class);
        Match match = org.mockito.Mockito.mock(Match.class);
        when(donor.getUser()).thenReturn(donorUser);
        when(donor.getName()).thenReturn("기부자");
        when(organization.getUser()).thenReturn(organizationUser);
        when(organization.getName()).thenReturn("기관");
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
