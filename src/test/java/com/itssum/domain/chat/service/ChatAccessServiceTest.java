package com.itssum.domain.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.repository.ChatParticipantRepository;
import com.itssum.domain.chat.repository.ChatRoomRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatAccessServiceTest {

    private final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    private final ChatParticipantRepository participants = mock(ChatParticipantRepository.class);
    private final ChatAccessService access = new ChatAccessService(rooms, participants);

    @Test
    void onlyParticipantMayOpenRoom() {
        when(rooms.findById(1L)).thenReturn(Optional.of(mock(ChatRoom.class)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> access.requireRoomForParticipant(1L, 10L));

        assertEquals(ErrorCode.CHAT_NOT_PARTICIPANT, error.getErrorCode());
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
