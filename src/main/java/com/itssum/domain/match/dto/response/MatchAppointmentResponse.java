package com.itssum.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itssum.domain.match.entity.DeliveryMethod;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchAppointmentResponse(
        Long matchId,
        MatchStatus status,
        DeliveryMethod deliveryMethod,
        LocalDateTime scheduledAt,
        String deliveryPlace,
        String receiverName
) {
    public static MatchAppointmentResponse from(Match match) {
        return new MatchAppointmentResponse(match.getId(), match.getStatus(), match.getDeliveryMethod(),
                match.getScheduledAt(), match.getDeliveryPlace(), match.getReceiverName());
    }
}
