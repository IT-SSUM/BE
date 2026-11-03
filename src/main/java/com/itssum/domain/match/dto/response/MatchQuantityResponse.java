package com.itssum.domain.match.dto.response;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;

public record MatchQuantityResponse(Long matchId, MatchStatus status,
                                    int quantity,
                                    int initialQuantity) {
    public static MatchQuantityResponse from(Match match) {
        return new MatchQuantityResponse(match.getId(), match.getStatus(), match.getQuantity(), match.getInitialQuantity());
    }
}
