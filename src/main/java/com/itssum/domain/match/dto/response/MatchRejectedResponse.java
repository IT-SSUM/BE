package com.itssum.domain.match.dto.response;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import java.time.LocalDateTime;

public record MatchRejectedResponse(
        Long matchId,
        MatchStatus status,
        LocalDateTime respondedAt
) {
    public static MatchRejectedResponse from(Match match) {
        return new MatchRejectedResponse(match.getId(), match.getStatus(), match.getRespondedAt());
    }
}
