package com.itssum.domain.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.domain.chat.event.ChatReadReceiptUpdatedEvent;

import com.itssum.domain.chat.dto.response.ChatMessagePageResponse;
import com.itssum.domain.chat.dto.response.ChatRoomResponse;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.chat.entity.ChatMessage;
import com.itssum.domain.chat.entity.ChatParticipant;
import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.chat.entity.MessageType;
import com.itssum.domain.chat.image.ChatImageStorage;
import com.itssum.domain.chat.repository.ChatMessageRepository;
import com.itssum.domain.chat.repository.ChatParticipantRepository;
import com.itssum.domain.user.entity.User;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;

class ChatQueryServiceTest {

    private final ChatAccessService accessService = mock(ChatAccessService.class);
    private final ChatParticipantRepository participants = mock(ChatParticipantRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ChatImageStorage> imageStorageProvider = mock(ObjectProvider.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ChatQueryService service = new ChatQueryService(
            accessService, participants, messages, imageStorageProvider, eventPublisher);

    @Test
    void readPointerAdvancesOnlyForMessageInRoom() {
        ChatParticipant participant = ChatParticipant.builder()
                .chatRoom(mock(ChatRoom.class)).user(mock(User.class)).joinedAt(LocalDateTime.now()).build();
        when(messages.findByIdAndChatRoom_Id(5L, 1L)).thenReturn(Optional.of(mock(ChatMessage.class)));
        when(participants.findByChatRoom_IdAndUser_Id(1L, 10L)).thenReturn(Optional.of(participant));

        service.markRead(1L, 10L, 5L);
        service.markRead(1L, 10L, 5L);

        assertEquals(5L, participant.getLastReadMessageId());
        ArgumentCaptor<ChatReadReceiptUpdatedEvent> event = ArgumentCaptor.forClass(ChatReadReceiptUpdatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(1L, event.getValue().chatRoomId());
        assertEquals(10L, event.getValue().readerId());
        assertEquals(5L, event.getValue().lastReadMessageId());
    }

    @Test
    void anotherRoomsMessageCannotBeMarkedRead() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.markRead(1L, 10L, 99L));

        assertEquals(ErrorCode.CHAT_MESSAGE_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void historyUsesCursorAndLimitsPageSize() {
        ChatRoom room = mock(ChatRoom.class);
        User sender = mock(User.class);
        when(sender.getId()).thenReturn(11L);
        ChatMessage newest = message(12L, room, sender);
        ChatMessage second = message(11L, room, sender);
        ChatMessage extra = message(10L, room, sender);
        when(messages.findRecent(eq(1L), eq(20L), any(Pageable.class)))
                .thenReturn(List.of(newest, second, extra));
        ChatParticipant other = mock(ChatParticipant.class);
        when(other.getLastReadMessageId()).thenReturn(11L);
        when(participants.findByChatRoom_IdAndUser_IdNot(1L, 10L)).thenReturn(Optional.of(other));

        ChatMessagePageResponse page = service.messages(1L, 10L, 20L, 2);

        assertEquals(2, page.messages().size());
        assertEquals(11L, page.nextCursor());
        assertEquals(true, page.hasNext());
        assertEquals(11L, page.otherLastReadMessageId());
        verify(accessService).requireRoomForParticipant(1L, 10L);
    }

    @Test
    void roomListContainsItemHeaderData() {
        ChatParticipant participant = mock(ChatParticipant.class);
        ChatRoom room = mock(ChatRoom.class);
        Match match = mock(Match.class);
        Donor donor = mock(Donor.class);
        Organization organization = mock(Organization.class);
        User viewer = mock(User.class);
        User other = mock(User.class);
        DonationItem item = mock(DonationItem.class);
        when(viewer.getId()).thenReturn(10L);
        when(donor.getUser()).thenReturn(viewer);
        when(organization.getUser()).thenReturn(other);
        when(match.getDonor()).thenReturn(donor);
        when(match.getOrganization()).thenReturn(organization);
        when(match.getDonationItem()).thenReturn(item);
        when(match.getQuantity()).thenReturn(30);
        when(item.getId()).thenReturn(123L);
        when(item.getItemName()).thenReturn("어린이 도서");
        when(item.getUnit()).thenReturn("권");
        when(room.getId()).thenReturn(1L);
        when(room.getMatch()).thenReturn(match);
        when(participant.getChatRoom()).thenReturn(room);
        when(participant.getUser()).thenReturn(viewer);
        when(participants.findAllByUser_Id(10L)).thenReturn(List.of(participant));

        ChatRoomResponse response = service.list(10L).getFirst();

        assertEquals(123L, response.donationItemId());
        assertEquals("어린이 도서", response.itemName());
        assertEquals(30, response.quantity());
        assertEquals("권", response.unit());
    }

    @Test
    void connectedNoticeUsesViewerSpecificTitle() {
        ChatRoom room = mock(ChatRoom.class);
        Match match = mock(Match.class);
        Donor donor = mock(Donor.class);
        User donorUser = mock(User.class);
        when(donorUser.getId()).thenReturn(10L);
        when(donor.getUser()).thenReturn(donorUser);
        when(match.getDonor()).thenReturn(donor);
        when(room.getMatch()).thenReturn(match);
        when(room.getId()).thenReturn(1L);
        ChatMessage notice = mock(ChatMessage.class);
        when(notice.getChatRoom()).thenReturn(room);
        when(notice.getMessageType()).thenReturn(MessageType.SYSTEM);
        when(notice.getContent()).thenReturn("MATCH_CONNECTED");
        when(messages.findRecent(eq(1L), eq(null), any(Pageable.class))).thenReturn(List.of(notice));

        ChatMessagePageResponse donorPage = service.messages(1L, 10L, null, 30);
        ChatMessagePageResponse organizationPage = service.messages(1L, 20L, null, 30);

        assertEquals("나눔이 연결됐어요 🌱", donorPage.messages().getFirst().content());
        assertEquals("물품 지원이 연결됐어요 🌱", organizationPage.messages().getFirst().content());
        assertEquals("전달 날짜·장소·방법을 함께 정해 주세요.", donorPage.messages().getFirst().systemDescription());
    }

    private ChatMessage message(Long id, ChatRoom room, User sender) {
        ChatMessage message = mock(ChatMessage.class);
        when(message.getId()).thenReturn(id);
        when(message.getChatRoom()).thenReturn(room);
        when(message.getSender()).thenReturn(sender);
        when(message.getMessageType()).thenReturn(MessageType.TEXT);
        when(message.getContent()).thenReturn("hi");
        when(room.getId()).thenReturn(1L);
        return message;
    }
}
