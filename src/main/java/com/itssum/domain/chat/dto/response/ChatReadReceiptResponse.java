package com.itssum.domain.chat.dto.response;

public record ChatReadReceiptResponse(
        Long chatRoomId,
        Long readerId,
        Long lastReadMessageId
) {
}
