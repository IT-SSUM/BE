package com.itssum.domain.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.domain.chat.dto.request.SendChatMessageRequest;
import com.itssum.domain.chat.dto.response.ChatMessageResponse;
import com.itssum.domain.chat.entity.ChatMessage;
import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.entity.MessageType;
import com.itssum.domain.chat.event.ChatMessageSentEvent;
import com.itssum.domain.chat.image.ChatImageStorage;
import com.itssum.domain.chat.repository.ChatMessageRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class ChatMessageServiceTest {

    private final ChatAccessService accessService = mock(ChatAccessService.class);
    private final ChatMessageRepository messageRepository = mock(ChatMessageRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ChatImageStorage> imageStorageProvider = mock(ObjectProvider.class);
    private final ChatMessageService service = new ChatMessageService(
            accessService, messageRepository, eventPublisher, imageStorageProvider);

    private final Match match = mock(Match.class);
    private final User user = User.builder().email("donor@example.com").password("hash")
            .role(UserRole.DONOR).build();
    private final ChatRoom room = ChatRoom.builder().match(match).build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(user, "id", 10L);
        ReflectionTestUtils.setField(room, "id", 1L);
        when(accessService.requireWritableRoom(1L, 10L)).thenReturn(room);
        when(match.getStatus()).thenReturn(MatchStatus.RESERVED);
        when(messageRepository.saveAndFlush(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 50L);
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 10, 28, 18, 0));
            return message;
        });
    }

    @Test
    void textMessageIsSavedAndPublished() {
        ChatMessageResponse result = service.send(1L, user,
                new SendChatMessageRequest(MessageType.TEXT, "  안녕하세요  "));

        assertEquals("안녕하세요", result.content());
        assertEquals("안녕하세요", room.getLastMessageContent());
        assertEquals(50L, result.messageId());
        verify(eventPublisher).publishEvent(any(ChatMessageSentEvent.class));
    }

    @Test
    void completedRoomIsReadOnly() {
        when(accessService.requireWritableRoom(1L, 10L))
                .thenThrow(new BusinessException(ErrorCode.CHAT_READ_ONLY));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.send(1L, user, new SendChatMessageRequest(MessageType.TEXT, "hello")));

        assertEquals(ErrorCode.CHAT_READ_ONLY, error.getErrorCode());
        verify(messageRepository, never()).saveAndFlush(any());
    }

    @Test
    void nonParticipantCannotSend() {
        when(accessService.requireWritableRoom(1L, 10L))
                .thenThrow(new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.send(1L, user, new SendChatMessageRequest(MessageType.TEXT, "hello")));

        assertEquals(ErrorCode.CHAT_NOT_PARTICIPANT, error.getErrorCode());
        verify(messageRepository, never()).saveAndFlush(any());
    }

    @Test
    void imageRequiresConfiguredStorage() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.send(1L, user, new SendChatMessageRequest(MessageType.IMAGE, "chat/1/a.jpg")));

        assertEquals(ErrorCode.CHAT_IMAGE_STORAGE_UNAVAILABLE, error.getErrorCode());
    }

    @Test
    void imageMessageChecksUploadAndReturnsTemporaryReadUrl() {
        ChatImageStorage storage = mock(ChatImageStorage.class);
        when(imageStorageProvider.getIfAvailable()).thenReturn(storage);
        String key = "chat/1/11111111-1111-1111-1111-111111111111.jpg";
        when(storage.presignRead(key)).thenReturn("https://example.com/signed-image");

        ChatMessageResponse result = service.send(1L, user,
                new SendChatMessageRequest(MessageType.IMAGE, key));

        verify(storage).verifyUploaded(1L, key);
        assertEquals("https://example.com/signed-image", result.content());
        assertEquals("[사진]", room.getLastMessageContent());
    }
}
