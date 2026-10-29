package com.itssum.domain.chat.service;

import com.itssum.domain.chat.dto.request.SendChatMessageRequest;
import com.itssum.domain.chat.dto.response.ChatMessageResponse;
import com.itssum.domain.chat.entity.ChatMessage;
import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.entity.MessageType;
import com.itssum.domain.chat.event.ChatMessageSentEvent;
import com.itssum.domain.chat.image.ChatImageStorage;
import com.itssum.domain.chat.repository.ChatMessageRepository;
import com.itssum.domain.user.entity.User;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatAccessService accessService;
    private final ChatMessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<ChatImageStorage> imageStorageProvider;

    @Transactional
    public ChatMessageResponse send(Long roomId, User sender, SendChatMessageRequest request) {
        ChatRoom room = accessService.requireWritableRoom(roomId, sender.getId());
        validate(request);
        String content = request.content().trim();
        ChatImageStorage imageStorage = null;
        if (request.messageType() == MessageType.IMAGE) {
            imageStorage = requireImageStorage();
            imageStorage.verifyUploaded(roomId, content);
        }
        ChatMessage message = messageRepository.saveAndFlush(ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .messageType(request.messageType())
                .content(content)
                .build());
        room.updateLastMessage(request.messageType() == MessageType.IMAGE ? "[사진]" : content,
                message.getCreatedAt());
        String displayContent = imageStorage == null ? content : imageStorage.presignRead(content);
        ChatMessageResponse response = ChatMessageResponse.from(message, displayContent);
        eventPublisher.publishEvent(new ChatMessageSentEvent(response));
        return response;
    }

    private void validate(SendChatMessageRequest request) {
        if (request == null || request.messageType() == null || request.content() == null
                || request.content().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (request.messageType() == MessageType.TEXT) {
            if (request.content().length() > 2000) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "메시지는 2000자 이하로 보내주세요.");
            }
        } else if (request.messageType() == MessageType.IMAGE) {
            if (request.content().length() > 255) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 식별자가 너무 깁니다.");
            }
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "보낼 수 없는 메시지 유형입니다.");
        }
    }

    private ChatImageStorage requireImageStorage() {
        ChatImageStorage storage = imageStorageProvider.getIfAvailable();
        if (storage == null) {
            throw new BusinessException(ErrorCode.CHAT_IMAGE_STORAGE_UNAVAILABLE);
        }
        return storage;
    }
}
