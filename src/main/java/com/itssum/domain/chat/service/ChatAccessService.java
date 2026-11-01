package com.itssum.domain.chat.service;

import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.repository.ChatParticipantRepository;
import com.itssum.domain.chat.repository.ChatRoomRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatAccessService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public ChatRoom requireRoomForParticipant(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!chatParticipantRepository.existsByChatRoom_IdAndUser_Id(roomId, userId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }
        return room;
    }

    @Transactional
    public ChatRoom requireWritableRoomForSend(Long roomId, Long userId) {
        ChatRoom current = requireRoomForParticipant(roomId, userId);
        Match match = matchRepository.findByIdForUpdate(current.getMatch().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        ChatRoom room = chatRoomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!chatParticipantRepository.existsByChatRoom_IdAndUser_Id(roomId, userId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }
        if (match.getStatus() != MatchStatus.TRADING && match.getStatus() != MatchStatus.RESERVED) {
            throw new BusinessException(ErrorCode.CHAT_READ_ONLY);
        }
        return room;
    }

    @Transactional(readOnly = true)
    public ChatRoom requireWritableRoom(Long roomId, Long userId) {
        return requireWritableStatus(requireRoomForParticipant(roomId, userId));
    }

    private ChatRoom requireWritableStatus(ChatRoom room) {
        MatchStatus status = room.getMatch().getStatus();
        if (status != MatchStatus.TRADING && status != MatchStatus.RESERVED) {
            throw new BusinessException(ErrorCode.CHAT_READ_ONLY);
        }
        return room;
    }
}
