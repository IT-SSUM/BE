package com.itssum.domain.chat.service;

import com.itssum.domain.chat.dto.response.ChatMessagePageResponse;
import com.itssum.domain.chat.dto.response.ChatMessageResponse;
import com.itssum.domain.chat.dto.response.ChatRoomResponse;
import com.itssum.domain.chat.entity.ChatMessage;
import com.itssum.domain.chat.entity.ChatParticipant;
import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.entity.ChatSystemNotice;
import com.itssum.domain.chat.entity.MessageType;
import com.itssum.domain.chat.event.ChatReadReceiptUpdatedEvent;
import com.itssum.domain.chat.image.ChatImageStorage;
import com.itssum.domain.chat.repository.ChatMessageRepository;
import com.itssum.domain.chat.repository.ChatParticipantRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatQueryService {

    private final ChatAccessService accessService;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final ObjectProvider<ChatImageStorage> imageStorageProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> list(Long userId) {
        return participantRepository.findAllByUser_Id(userId).stream()
                .map(participant -> toRoomResponse(participant.getChatRoom(), participant))
                .sorted(Comparator.comparing(ChatRoomResponse::lastMessageAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ChatRoomResponse::chatRoomId, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse messages(Long roomId, Long userId, Long beforeId, int size) {
        accessService.requireRoomForParticipant(roomId, userId);
        if (size < 1 || size > 100 || (beforeId != null && beforeId < 1)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        List<ChatMessage> page = messageRepository.findRecent(roomId, beforeId, PageRequest.of(0, size + 1));
        boolean hasNext = page.size() > size;
        List<ChatMessage> selected = page.stream().limit(size).toList();
        List<ChatMessageResponse> messages = selected.stream()
                .map(message -> toMessageResponse(message, userId)).toList();
        Long nextCursor = hasNext ? selected.getLast().getId() : null;
        Long otherLastReadMessageId = participantRepository.findByChatRoom_IdAndUser_IdNot(roomId, userId)
                .map(ChatParticipant::getLastReadMessageId)
                .orElse(null);
        return new ChatMessagePageResponse(messages, nextCursor, hasNext, otherLastReadMessageId);
    }

    @Transactional
    public void markRead(Long roomId, Long userId, Long lastReadMessageId) {
        accessService.requireRoomForParticipant(roomId, userId);
        if (lastReadMessageId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "마지막으로 읽은 메시지 ID가 필요합니다.");
        }
        messageRepository.findByIdAndChatRoom_Id(lastReadMessageId, roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
        ChatParticipant participant = participantRepository.findByChatRoom_IdAndUser_Id(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT));
        Long previous = participant.getLastReadMessageId();
        participant.markReadThrough(lastReadMessageId);
        if (previous == null || lastReadMessageId > previous) {
            eventPublisher.publishEvent(new ChatReadReceiptUpdatedEvent(roomId, userId, lastReadMessageId));
        }
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, ChatParticipant participant) {
        Match match = room.getMatch();
        boolean donor = match.getDonor().getUser().getId().equals(participant.getUser().getId());
        Long otherUserId = donor ? match.getOrganization().getUser().getId()
                : match.getDonor().getUser().getId();
        String otherName = donor ? match.getOrganization().getName() : match.getDonor().getName();
        long unread = messageRepository.countUnread(
                room.getId(), participant.getLastReadMessageId() == null ? 0L : participant.getLastReadMessageId(),
                participant.getUser().getId());
        String preview = room.getLastMessageContent();
        if (ChatSystemNotice.MATCH_CONNECTED.name().equals(preview)) {
            preview = ChatSystemNotice.MATCH_CONNECTED.title(donor);
        }
        return new ChatRoomResponse(room.getId(), match.getId(), otherUserId, otherName,
                match.getDonationItem().getId(), match.getDonationItem().getItemName(),
                match.getQuantity(), match.getDonationItem().getUnit(), match.getStatus(), preview,
                room.getLastMessageAt(), unread);
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message, Long viewerId) {
        String content = message.getContent();
        if (message.getMessageType() == MessageType.SYSTEM
                && ChatSystemNotice.MATCH_CONNECTED.name().equals(content)) {
            boolean donor = message.getChatRoom().getMatch().getDonor().getUser().getId().equals(viewerId);
            return ChatMessageResponse.from(message, ChatSystemNotice.MATCH_CONNECTED.title(donor),
                    ChatSystemNotice.MATCH_CONNECTED.description());
        }
        if (message.getMessageType() == MessageType.IMAGE) {
            ChatImageStorage storage = imageStorageProvider.getIfAvailable();
            if (storage == null) {
                throw new BusinessException(ErrorCode.CHAT_IMAGE_STORAGE_UNAVAILABLE);
            }
            content = storage.presignRead(content);
        }
        return ChatMessageResponse.from(message, content);
    }
}
