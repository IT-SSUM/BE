package com.itssum.domain.match.dto.response;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import java.time.LocalDateTime;

public record MatchCompletedResponse(Long matchId, MatchStatus status, int quantity, LocalDateTime completedAt) {
    public static MatchCompletedResponse from(Match match) {
        return new MatchCompletedResponse(match.getId(), match.getStatus(), match.getQuantity(), match.getCompletedAt());
    }
}
