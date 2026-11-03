package com.itssum.domain.match.dto.response;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import java.time.LocalDateTime;

public record MatchCreatedResponse(
        Long matchId,
        MatchStatus status,
        LocalDateTime createdAt
) {
    public static MatchCreatedResponse from(Match match) {
        return new MatchCreatedResponse(match.getId(), match.getStatus(), match.getCreatedAt());
    }
}
