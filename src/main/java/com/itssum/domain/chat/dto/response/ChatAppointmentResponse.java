package com.itssum.domain.chat.dto.response;

import com.itssum.domain.match.entity.DeliveryMethod;
import com.itssum.domain.match.entity.Match;
import java.time.LocalDateTime;

public record ChatAppointmentResponse(
        DeliveryMethod deliveryMethod,
        LocalDateTime scheduledAt,
        String deliveryPlace,
        String receiverName
) {
    public static ChatAppointmentResponse from(Match match) {
        return new ChatAppointmentResponse(match.getDeliveryMethod(), match.getScheduledAt(),
                match.getDeliveryPlace(), match.getReceiverName());
    }
}
