package com.itssum.domain.user.dto.response;

import com.itssum.domain.item.entity.Region;
import com.itssum.domain.user.entity.Donor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record DonorProfileResponse(
        @Schema(description = "로그인 이메일 (수정 불가)", example = "donor@example.com") String email,
        @Schema(description = "이름", example = "최윤서") String name,
        @Schema(description = "생년월일", example = "1999-03-02") LocalDate birthDate,
        @Schema(description = "전화번호", example = "010-1234-5678") String phone,
        @Schema(description = "활동 지역 ID (경기 부천시 고정)", example = "1") Long regionId,
        @Schema(description = "활동 지역 표시용 이름", example = "경기 부천시") String regionName
) {

    public static DonorProfileResponse from(Donor donor) {
        Region region = donor.getRegion();
        return new DonorProfileResponse(donor.getUser().getEmail(), donor.getName(), donor.getBirthDate(),
                donor.getPhone(), region == null ? null : region.getId(),
                region == null ? null : region.getSido() + " " + region.getSigungu());
    }
}
