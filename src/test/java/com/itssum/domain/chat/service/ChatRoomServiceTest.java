package com.itssum.domain.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.domain.chat.entity.ChatMessage;
import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.entity.MessageType;
import com.itssum.domain.chat.repository.ChatMessageRepository;
import com.itssum.domain.chat.repository.ChatParticipantRepository;
import com.itssum.domain.chat.repository.ChatRoomRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChatRoomServiceTest {

    private final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    private final ChatParticipantRepository participants = mock(ChatParticipantRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final ChatRoomService service = new ChatRoomService(rooms, participants, messages);

    @Test
    void reservedMatchCreatesConnectedNoticeOnce() {
        Match match = mock(Match.class);
        when(match.getId()).thenReturn(7L);
        when(match.getStatus()).thenReturn(MatchStatus.RESERVED);
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

        service.createForReservedMatch(match);
        service.createForReservedMatch(match);

        ArgumentCaptor<ChatMessage> created = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages).saveAndFlush(created.capture());
        assertEquals(MessageType.SYSTEM, created.getValue().getMessageType());
        assertEquals("MATCH_CONNECTED", created.getValue().getContent());
        verify(room).updateLastMessage("MATCH_CONNECTED", createdAt);
    }
}
