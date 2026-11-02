package com.itssum.domain.item.dto.response;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.DonationItemStatus;
import com.itssum.domain.user.entity.Donor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Schema(description = "기부 물품 상세. 등록한 기부자에게만 주는 값(물품 위치·상세 주소·사진 key)은 다른 회원에게 null 이다")
public record DonationItemDetailResponse(
        @Schema(description = "기부 물품 ID", example = "10") Long donationItemId,
        @Schema(description = "RECRUITING(모집중) | FULL(예약마감) | COMPLETED(전달 완료)", example = "RECRUITING")
        DonationItemStatus status,
        @Schema(description = "카테고리 ID", example = "5") Long categoryId,
        @Schema(description = "카테고리 이름", example = "도서·문구") String categoryName,
        @Schema(description = "품목", example = "어린이 도서") String itemName,
        @Schema(description = "총 수량", example = "30") int quantity,
        @Schema(description = "예약 수량", example = "10") int reservedQuantity,
        @Schema(description = "전달 완료 수량", example = "0") int completedQuantity,
        @Schema(description = "신청 가능 수량 (총 수량 − 예약 − 전달 완료)", example = "20") int availableQuantity,
        @Schema(description = "단위", example = "권") String unit,
        @Schema(description = "물품 상태", example = "MID") ConditionGrade conditionGrade,
        @Schema(description = "설명", example = "초등학생이 읽기 좋은 도서예요.") String description,
        @Schema(description = "물품 지역 표시용 이름 (구 단위)", example = "경기 부천시") String regionName,
        @Schema(description = "사진 조회 URL (1시간 유효, 대표 사진부터). 저장소가 없는 환경에서는 빈 목록") List<String> imageUrls,
        @Schema(description = "등록 일시") LocalDateTime createdAt,
        @Schema(description = "내가 등록한 물품인지", example = "true") boolean mine,
        @Schema(description = "기부자 회원 ID", example = "1") Long donorId,
        @Schema(description = "기부자 이름", example = "최윤서") String donorName,
        @Schema(description = "기부자 소개", example = "이웃에게 필요한 물품을 전하고 있어요.") String donorIntroduction,
        @Schema(description = "물품 위치 (등록한 기부자에게만)", example = "경기 부천시 길주로 210") String pickupAddress,
        @Schema(description = "상세 주소 (등록한 기부자에게만)", example = "건물 입구에서 전달할게요") String pickupAddressDetail,
        @Schema(description = "사진 key (등록한 기부자에게만). 사진을 유지한 채 수정할 때 이 값을 다시 보낸다") List<String> imageKeys
) {
    public static DonationItemDetailResponse of(DonationItem item, boolean mine, Function<String, String> imageUrl) {
        Donor donor = item.getDonor();
        List<String> keys = item.imageKeys();
        return new DonationItemDetailResponse(item.getId(), item.getStatus(), item.getCategory().getId(),
                item.getCategory().getName(), item.getItemName(), item.getQuantity(), item.getReservedQuantity(),
                item.getCompletedQuantity(), item.availableQuantity(), item.getUnit(), item.getConditionGrade(),
                item.getDescription(), item.getRegion().displayName(),
                keys.stream().map(imageUrl).filter(Objects::nonNull).toList(), item.getCreatedAt(), mine,
                donor.getId(), donor.getName(), donor.getIntroduction(),
                mine ? item.getPickupAddress() : null, mine ? item.getPickupAddressDetail() : null, mine ? keys : null);
    }
}
