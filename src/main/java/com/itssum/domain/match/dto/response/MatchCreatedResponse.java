package com.itssum.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.entity.RequesterType;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchCreatedResponse(
        Long matchId,
        Long donationItemId,
        String donationItemName,
        Long neededItemId,
        String neededItemName,
        Long donorId,
        Long organizationId,
        RequesterType requestedBy,
        int quantity,
        String message,
        MatchStatus status,
        LocalDateTime createdAt
) {
    public static MatchCreatedResponse from(Match match) {
        return new MatchCreatedResponse(
                match.getId(), match.getDonationItem().getId(), match.getDonationItem().getItemName(),
                match.getNeededItem().getId(), match.getNeededItem().getItemName(),
                match.getDonor().getId(), match.getOrganization().getId(), match.getRequestedBy(),
                match.getQuantity(), match.getMessage(), match.getStatus(), match.getCreatedAt());
    }
}
