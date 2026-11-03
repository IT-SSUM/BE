package com.itssum.domain.match.dto.response;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import java.time.LocalDateTime;

public record MatchAcceptedResponse(
        Long matchId,
        Long chatRoomId,
        MatchStatus status,
        LocalDateTime respondedAt
) {
    public static MatchAcceptedResponse from(Match match, Long chatRoomId) {
        return new MatchAcceptedResponse(match.getId(), chatRoomId, match.getStatus(), match.getRespondedAt());
    }
}
