package com.itssum.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.domain.chat.entity.ChatMessage;
import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.entity.MessageType;
import com.itssum.domain.chat.event.ChatMessageSentEvent;
import com.itssum.domain.chat.repository.ChatMessageRepository;
import com.itssum.domain.chat.repository.ChatParticipantRepository;
import com.itssum.domain.chat.repository.ChatRoomRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.DeliveryMethod;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ChatRoomServiceTest {

    private final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    private final ChatParticipantRepository participants = mock(ChatParticipantRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ChatRoomService service = new ChatRoomService(
            rooms, participants, messages, events, new ChatAppointmentCodec());

    @Test
    void tradingMatchCreatesConnectedNoticeOnce() {
        Match match = mock(Match.class);
        when(match.getId()).thenReturn(7L);
        when(match.getStatus()).thenReturn(MatchStatus.TRADING);
        Donor donor = mock(Donor.class);
        Organization organization = mock(Organization.class);
        when(donor.getUser()).thenReturn(mock(User.class));
        when(organization.getUser()).thenReturn(mock(User.class));
        when(match.getDonor()).thenReturn(donor);
        when(match.getOrganization()).thenReturn(organization);
        ChatRoom room = mock(ChatRoom.class);
        when(rooms.save(any(ChatRoom.class))).thenReturn(room);
        ChatMessage saved = mock(ChatMessage.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 29, 12, 0);
        when(saved.getCreatedAt()).thenReturn(createdAt);
        when(messages.saveAndFlush(any(ChatMessage.class))).thenReturn(saved);
        when(rooms.findByMatch_Id(7L)).thenReturn(Optional.empty(), Optional.of(room));

        service.createForMatch(match);
        service.createForMatch(match);

        ArgumentCaptor<ChatMessage> created = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages).saveAndFlush(created.capture());
        assertEquals(MessageType.SYSTEM, created.getValue().getMessageType());
        assertEquals("MATCH_CONNECTED", created.getValue().getContent());
        verify(room).updateLastMessage("MATCH_CONNECTED", createdAt);
    }

    @Test
    void confirmedAppointmentCreatesCardAndPublishesEvent() throws Exception {
        Match match = mock(Match.class);
        when(match.getId()).thenReturn(7L);
        when(match.getDeliveryMethod()).thenReturn(DeliveryMethod.DIRECT);
        when(match.getScheduledAt()).thenReturn(LocalDateTime.of(2026, 11, 2, 14, 0));
        when(match.getDeliveryPlace()).thenReturn("기관 입구");
        ChatRoom room = mock(ChatRoom.class);
        when(room.getId()).thenReturn(12L);
        when(rooms.findByMatch_Id(7L)).thenReturn(Optional.of(room));
        when(rooms.findByIdForUpdate(12L)).thenReturn(Optional.of(room));
        ChatMessage saved = mock(ChatMessage.class);
        when(saved.getId()).thenReturn(44L);
        when(saved.getChatRoom()).thenReturn(room);
        when(saved.getMessageType()).thenReturn(MessageType.APPOINTMENT);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 30, 15, 0);
        when(saved.getCreatedAt()).thenReturn(createdAt);
        when(messages.saveAndFlush(any(ChatMessage.class))).thenReturn(saved);

        User confirmer = mock(User.class);
        service.createAppointmentCard(match, confirmer);

        ArgumentCaptor<ChatMessage> card = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages).saveAndFlush(card.capture());
        assertEquals(MessageType.APPOINTMENT, card.getValue().getMessageType());
        assertEquals(confirmer, card.getValue().getSender());
        assertEquals("기관 입구", objectMapper.readTree(card.getValue().getContent()).get("deliveryPlace").asText());
        assertEquals("2026-11-02T14:00:00", objectMapper.readTree(card.getValue().getContent())
                .get("scheduledAt").asText());
        verify(room).updateLastMessage("전달 약속", createdAt);
        ArgumentCaptor<ChatMessageSentEvent> event = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals("기관 입구", event.getValue().message().appointment().deliveryPlace());
    }
}
