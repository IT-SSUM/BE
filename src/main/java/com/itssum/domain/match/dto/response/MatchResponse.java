package com.itssum.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.match.entity.DeliveryMethod;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.entity.RequesterType;
import com.itssum.domain.user.entity.Organization;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchResponse(
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
        DeliveryMethod deliveryMethod,
        LocalDateTime scheduledAt,
        String deliveryPlace,
        String receiverName,
        LocalDateTime createdAt,
        LocalDateTime respondedAt,
        LocalDateTime reservedAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        String cancelReason,
        @Schema(description = "물품 카테고리 ID") Long itemCategoryId,
        @Schema(description = "물품 카테고리명") String itemCategoryName,
        @Schema(description = "거래 수량의 단위") String unit,
        @Schema(description = "기부 물품 상태") ConditionGrade conditionGrade,
        @Schema(description = "기부 물품의 구 단위 위치") String donationItemRegion,
        @Schema(description = "기부 물품의 현재 신청 가능 수량") int donationItemAvailableQuantity,
        @Schema(description = "필요 물품의 구 단위 수령 지역") String neededItemRegion,
        @Schema(description = "기관의 현재 추가 필요 수량") int neededItemAdditionalQuantity,
        @Schema(description = "필요 물품에 등록한 수령 장소 상세 주소") String neededItemReceiveAddress,
        @Schema(description = "기부자 이름") String donorName,
        @Schema(description = "기관 이름") String organizationName,
        @Schema(description = "기관 주소") String organizationAddress,
        @Schema(description = "수락 후 생성된 채팅방 ID. 응답 대기·거절 상태에서는 없음") Long chatRoomId
) {
    public static MatchResponse from(Match match, Long chatRoomId) {
        DonationItem donation = match.getDonationItem();
        NeededItem needed = match.getNeededItem();
        Organization organization = match.getOrganization();
        return new MatchResponse(
                match.getId(), donation.getId(), donation.getItemName(),
                needed.getId(), needed.getItemName(),
                match.getDonor().getId(), organization.getId(), match.getRequestedBy(),
                match.getQuantity(), match.getMessage(), match.getStatus(), match.getDeliveryMethod(),
                match.getScheduledAt(), match.getDeliveryPlace(), match.getReceiverName(),
                match.getCreatedAt(), match.getRespondedAt(), match.getReservedAt(),
                match.getCompletedAt(), match.getCanceledAt(), match.getCancelReason(),
                donation.getCategory().getId(), donation.getCategory().getName(), donation.getUnit(),
                donation.getConditionGrade(), regionName(donation.getRegion()), donation.availableQuantity(),
                regionName(needed.getRegion()), needed.availableQuantity(), needed.getReceiveAddress(),
                match.getDonor().getName(), organization.getName(), organizationAddress(organization), chatRoomId);
    }

    private static String regionName(Region region) {
        return region == null ? null : region.getSido() + " " + region.getSigungu();
    }

    private static String organizationAddress(Organization organization) {
        String address = organization.getAddress();
        String detail = organization.getAddressDetail();
        return address == null || detail == null || detail.isBlank() ? address : address + " " + detail;
    }
}
