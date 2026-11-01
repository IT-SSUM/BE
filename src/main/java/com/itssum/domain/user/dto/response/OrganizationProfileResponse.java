package com.itssum.domain.user.dto.response;

import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record OrganizationProfileResponse(
        @Schema(description = "로그인 이메일 (수정 불가)", example = "org@example.com") String email,
        @Schema(description = "기관명", example = "구로지역아동센터") String name,
        @Schema(description = "대표자명 (수정 불가)", example = "김수현") String representativeName,
        @Schema(description = "사업자등록번호 10자리, 하이픈 없음 (수정 불가)", example = "1234567890") String businessNumber,
        @Schema(description = "개업일자 (수정 불가)", example = "2015-03-02") LocalDate openingDate,
        @Schema(description = "기관 연락처", example = "02-1234-5678") String phone,
        @Schema(description = "기관 주소", example = "서울 구로구 가마산로 12") String address,
        @Schema(description = "상세주소", example = "2층") String addressDetail,
        @Schema(description = "사업자 인증 상태", example = "VERIFIED") VerificationStatus verificationStatus
) {

    public static OrganizationProfileResponse from(Organization organization) {
        return new OrganizationProfileResponse(organization.getUser().getEmail(), organization.getName(),
                organization.getRepresentativeName(), organization.getBusinessNumber(), organization.getOpeningDate(),
                organization.getPhone(), organization.getAddress(), organization.getAddressDetail(),
                organization.getVerificationStatus());
    }
}
