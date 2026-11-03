package com.itssum.domain.match.dto.response;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import java.time.LocalDateTime;

public record MatchReservedResponse(Long matchId, MatchStatus status, int quantity, LocalDateTime reservedAt) {
    public static MatchReservedResponse from(Match match) {
        return new MatchReservedResponse(match.getId(), match.getStatus(), match.getQuantity(), match.getReservedAt());
    }
}
