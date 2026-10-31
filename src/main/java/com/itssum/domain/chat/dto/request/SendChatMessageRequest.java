package com.itssum.domain.chat.dto.request;

import com.itssum.domain.chat.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendChatMessageRequest(
        @NotNull MessageType messageType,
        @NotBlank String content
) {
}
