package com.itssum.domain.chat.event;

import com.itssum.domain.chat.dto.response.ChatReadReceiptResponse;

public record ChatReadReceiptUpdatedEvent(
        Long chatRoomId,
        Long readerId,
        Long lastReadMessageId
) {
    public ChatReadReceiptResponse response() {
        return new ChatReadReceiptResponse(chatRoomId, readerId, lastReadMessageId);
    }
}
