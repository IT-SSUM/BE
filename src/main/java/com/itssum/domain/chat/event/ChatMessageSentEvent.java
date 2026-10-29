package com.itssum.domain.chat.event;

import com.itssum.domain.chat.dto.response.ChatMessageResponse;

public record ChatMessageSentEvent(ChatMessageResponse message) {
}
