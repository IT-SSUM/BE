package com.itssum.domain.chat.service;

import com.itssum.domain.chat.dto.response.ChatAppointmentResponse;
import com.itssum.domain.chat.dto.response.ChatMessageResponse;
import com.itssum.domain.chat.entity.ChatParticipant;
import com.itssum.domain.chat.entity.ChatMessage;
import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.entity.ChatSystemNotice;
import com.itssum.domain.chat.entity.MessageType;
import com.itssum.domain.chat.event.ChatMessageSentEvent;
import com.itssum.domain.chat.repository.ChatMessageRepository;
import com.itssum.domain.chat.repository.ChatParticipantRepository;
import com.itssum.domain.chat.repository.ChatRoomRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.user.entity.User;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatAppointmentCodec appointmentCodec;

    /** 상대방이 제안·요청을 수락한 트랜잭션에서 호출한다. 한 거래에 방은 하나만 만든다. */
    @Transactional
    public ChatRoom createForMatch(Match match) {
        if (match.getStatus() != MatchStatus.TRADING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "거래중인 매칭만 채팅방을 만들 수 있습니다.");
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

    @Transactional
    public void createAppointmentCard(Match match, User confirmedBy) {
        ChatRoom existing = chatRoomRepository.findByMatch_Id(match.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        ChatRoom room = chatRoomRepository.findByIdForUpdate(existing.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        ChatAppointmentResponse appointment = ChatAppointmentResponse.from(match);
        String snapshot = appointmentCodec.encode(appointment);
        ChatMessage message = messageRepository.saveAndFlush(ChatMessage.builder()
                .chatRoom(room).sender(confirmedBy).messageType(MessageType.APPOINTMENT)
                .content(snapshot).build());
        room.updateLastMessage("전달 약속", message.getCreatedAt());
        ChatMessageResponse response = ChatMessageResponse.from(message, "전달 약속", null,
                appointment);
        eventPublisher.publishEvent(new ChatMessageSentEvent(response));
    }
}
