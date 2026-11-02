package com.itssum.domain.item.dto.response;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.NeededItemStatus;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.function.Function;

@Schema(description = "필요 물품 상세. 등록한 기관에게만 주는 값(수령 장소·참고 사진 key)은 다른 회원에게 null 이다")
public record NeededItemDetailResponse(
        @Schema(description = "필요 물품 ID", example = "20") Long neededItemId,
        @Schema(description = "RECRUITING(모집중) | FULL(예약마감) | CLOSED(모집 종료)", example = "RECRUITING")
        NeededItemStatus status,
        @Schema(description = "카테고리 ID", example = "1") Long categoryId,
        @Schema(description = "카테고리 이름", example = "생활용품") String categoryName,
        @Schema(description = "품목", example = "생수") String itemName,
        @Schema(description = "필요 수량", example = "20") int requiredQuantity,
        @Schema(description = "예약 수량", example = "10") int reservedQuantity,
        @Schema(description = "수령 완료 수량", example = "5") int receivedQuantity,
        @Schema(description = "추가 필요 수량 (필요 수량 − 예약 − 수령 완료)", example = "5") int availableQuantity,
        @Schema(description = "단위", example = "병") String unit,
        @Schema(description = "희망 물품 상태 (이 등급 이상)", example = "HIGH") ConditionGrade desiredCondition,
        @Schema(description = "필요한 이유", example = "아이들이 편하게 마실 생수가 필요해요.") String description,
        @Schema(description = "수령 지역 표시용 이름", example = "경기 부천시") String regionName,
        @Schema(description = "참고 사진 조회 URL (1시간 유효). 없거나 저장소가 없는 환경에서는 null") String referenceImageUrl,
        @Schema(description = "등록 일시") LocalDateTime createdAt,
        @Schema(description = "내가 등록한 물품인지", example = "false") boolean mine,
        @Schema(description = "기관 회원 ID", example = "2") Long organizationId,
        @Schema(description = "기관명", example = "구로지역아동센터") String organizationName,
        @Schema(description = "기관 인증 상태 (VERIFIED = 사업자 확인 완료)", example = "VERIFIED")
        VerificationStatus verificationStatus,
        @Schema(description = "기관 주소 (기관 위치)", example = "서울 구로구 가마산로 12") String organizationAddress,
        @Schema(description = "기관 소개", example = "아이들의 일상과 성장을 함께하는 지역 돌봄 기관이에요.")
        String organizationIntroduction,
        @Schema(description = "수령 장소 (등록한 기관에게만)", example = "서울 구로구 가마산로 12") String receiveAddress,
        @Schema(description = "참고 사진 key (등록한 기관에게만). 사진을 유지한 채 수정할 때 이 값을 다시 보낸다")
        String referenceImageKey
) {
    public static NeededItemDetailResponse of(NeededItem item, boolean mine, Function<String, String> imageUrl) {
        Organization organization = item.getOrganization();
        String referenceImageKey = item.getReferenceImageUrl();
        return new NeededItemDetailResponse(item.getId(), item.getStatus(), item.getCategory().getId(),
                item.getCategory().getName(), item.getItemName(), item.getRequiredQuantity(), item.getReservedQuantity(),
                item.getReceivedQuantity(), item.availableQuantity(), item.getUnit(), item.getDesiredCondition(),
                item.getDescription(), item.getRegion().displayName(),
                referenceImageKey == null ? null : imageUrl.apply(referenceImageKey), item.getCreatedAt(), mine,
                organization.getId(), organization.getName(), organization.getVerificationStatus(),
                organization.getAddress(), organization.getIntroduction(),
                mine ? item.getReceiveAddress() : null, mine ? referenceImageKey : null);
    }
}
