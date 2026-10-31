package com.itssum.domain.chat.service;

import com.itssum.domain.chat.entity.ChatParticipant;
import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.entity.ChatMessage;
import com.itssum.domain.chat.entity.ChatSystemNotice;
import com.itssum.domain.chat.entity.MessageType;
import com.itssum.domain.chat.repository.ChatMessageRepository;
import com.itssum.domain.chat.repository.ChatParticipantRepository;
import com.itssum.domain.chat.repository.ChatRoomRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;

    /** 매칭 수락 트랜잭션에서 호출한다. 한 매칭에 방은 하나만 만든다. */
    @Transactional
    public ChatRoom createForReservedMatch(Match match) {
        if (match.getStatus() != MatchStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "예약된 매칭만 채팅방을 만들 수 있습니다.");
        }
        return chatRoomRepository.findByMatch_Id(match.getId()).orElseGet(() -> {
            ChatRoom room = chatRoomRepository.save(ChatRoom.builder().match(match).build());
            LocalDateTime now = LocalDateTime.now();
            participantRepository.save(ChatParticipant.builder()
                    .chatRoom(room).user(match.getDonor().getUser()).joinedAt(now).build());
            participantRepository.save(ChatParticipant.builder()
                    .chatRoom(room).user(match.getOrganization().getUser()).joinedAt(now).build());
            ChatMessage notice = messageRepository.saveAndFlush(ChatMessage.builder()
                    .chatRoom(room)
                    .messageType(MessageType.SYSTEM)
                    .content(ChatSystemNotice.MATCH_CONNECTED.name())
                    .build());
            room.updateLastMessage(ChatSystemNotice.MATCH_CONNECTED.name(), notice.getCreatedAt());
            return room;
        });
    }
}
