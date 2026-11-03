package com.itssum.domain.notification.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.notification.dto.response.NotificationPageResponse;
import com.itssum.domain.notification.dto.response.NotificationResponse;
import com.itssum.domain.notification.entity.NotificationTargetType;
import com.itssum.domain.notification.entity.NotificationType;
import com.itssum.domain.notification.dto.response.UnreadNotificationCountResponse;
import com.itssum.domain.notification.service.NotificationService;
import com.itssum.domain.notification.service.NotificationUserResolver;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.security.AccessTokenProvider;
import com.itssum.support.SecurityTestConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import(NotificationUserResolver.class)
@SecurityTestConfig
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider tokens;

    @MockitoBean
    private NotificationService notifications;

    @MockitoBean
    private UserRepository users;

    @Test
    void notificationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanListCountAndReadOwnNotifications() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.isActive()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(notifications.list(7L, null, 30)).thenReturn(new NotificationPageResponse(List.of(
                new NotificationResponse(9L, NotificationType.MATCH_ACCEPTED, "기부 제안·요청 수락",
                        "도서 3권", NotificationTargetType.MATCH, 12L, false,
                        LocalDateTime.of(2026, 11, 1, 18, 0))), null, false));
        when(notifications.unreadCount(7L)).thenReturn(new UnreadNotificationCountResponse(2));
        String authorization = "Bearer " + tokens.issue(7L, UserRole.DONOR);

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_200"))
                .andExpect(jsonPath("$.data.notifications").isArray())
                .andExpect(jsonPath("$.data.notifications[0].targetId").value(12))
                .andExpect(jsonPath("$.data.notifications[0].isRead").value(false))
                .andExpect(jsonPath("$.data.notifications[0].readAt").doesNotExist());
        mockMvc.perform(get("/api/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2));
        mockMvc.perform(patch("/api/notifications/9/read").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_200_2"));
        mockMvc.perform(patch("/api/notifications/read-all").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_200_3"));
        verify(notifications).markRead(9L, 7L);
        verify(notifications).markAllRead(7L);
    }
}
