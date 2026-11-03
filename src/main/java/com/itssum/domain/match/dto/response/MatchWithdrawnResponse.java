package com.itssum.domain.match.dto.response;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import java.time.LocalDateTime;

public record MatchWithdrawnResponse(
        Long matchId,
        MatchStatus status,
        LocalDateTime canceledAt
) {
    public static MatchWithdrawnResponse from(Match match) {
        return new MatchWithdrawnResponse(match.getId(), match.getStatus(), match.getCanceledAt());
    }
}
