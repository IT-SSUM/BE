package com.itssum.domain.match.dto.response;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import java.time.LocalDateTime;

public record MatchCanceledResponse(Long matchId, MatchStatus status, LocalDateTime canceledAt) {
    public static MatchCanceledResponse from(Match match) {
        return new MatchCanceledResponse(match.getId(), match.getStatus(), match.getCanceledAt());
    }
}
