package com.itssum.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.match.entity.DeliveryMethod;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.entity.RequesterType;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchListResponse(
        Long matchId,
        RequesterType requestedBy,
        MatchStatus status,
        String donationItemName,
        String neededItemName,
        String donorName,
        String organizationName,
        int quantity,
        String unit,
        Long itemCategoryId,
        String donationItemRegion,
        String neededItemRegion,
        DeliveryMethod deliveryMethod,
        LocalDateTime scheduledAt,
        LocalDateTime completedAt
) {
    public static MatchListResponse from(Match match) {
        DonationItem donation = match.getDonationItem();
        NeededItem needed = match.getNeededItem();
        return new MatchListResponse(match.getId(), match.getRequestedBy(), match.getStatus(),
                donation.getItemName(), needed.getItemName(), match.getDonor().getName(),
                match.getOrganization().getName(), match.getQuantity(), donation.getUnit(),
                donation.getCategory().getId(), regionName(donation.getRegion()), regionName(needed.getRegion()),
                match.getDeliveryMethod(), match.getScheduledAt(), match.getCompletedAt());
    }

    private static String regionName(Region region) {
        return region == null ? null : region.getSido() + " " + region.getSigungu();
    }
}
