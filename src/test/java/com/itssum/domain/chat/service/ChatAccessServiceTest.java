package com.itssum.domain.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.repository.ChatParticipantRepository;
import com.itssum.domain.chat.repository.ChatRoomRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ChatAccessServiceTest {

    private final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    private final ChatParticipantRepository participants = mock(ChatParticipantRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final ChatAccessService access = new ChatAccessService(rooms, participants, matches);

    @Test
    void onlyParticipantMayOpenRoom() {
        when(rooms.findById(1L)).thenReturn(Optional.of(mock(ChatRoom.class)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> access.requireRoomForParticipant(1L, 10L));

        assertEquals(ErrorCode.CHAT_NOT_PARTICIPANT, error.getErrorCode());
    }

    @Test
    void sendingLocksRoomAndChecksParticipantAndStatus() {
        Match match = mock(Match.class);
        ChatRoom room = mock(ChatRoom.class);
        when(room.getMatch()).thenReturn(match);
        when(match.getId()).thenReturn(7L);
        when(match.getStatus()).thenReturn(MatchStatus.RESERVED);
        when(rooms.findById(1L)).thenReturn(Optional.of(room));
        when(matches.findByIdForUpdate(7L)).thenReturn(Optional.of(match));
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoom_IdAndUser_Id(1L, 10L)).thenReturn(true);

        assertEquals(room, access.requireWritableRoomForSend(1L, 10L));
        InOrder order = inOrder(matches, rooms);
        order.verify(matches).findByIdForUpdate(7L);
        order.verify(rooms).findByIdForUpdate(1L);
    }

    @Test
    void completedMatchCanBeReadButNotWritten() {
        Match match = mock(Match.class);
        ChatRoom room = mock(ChatRoom.class);
        when(room.getMatch()).thenReturn(match);
        when(match.getStatus()).thenReturn(MatchStatus.COMPLETED);
        when(rooms.findById(1L)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoom_IdAndUser_Id(1L, 10L)).thenReturn(true);

        assertEquals(room, access.requireRoomForParticipant(1L, 10L));
        BusinessException error = assertThrows(BusinessException.class,
                () -> access.requireWritableRoom(1L, 10L));

        assertEquals(ErrorCode.CHAT_READ_ONLY, error.getErrorCode());
    }
}
