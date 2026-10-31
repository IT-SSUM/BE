package com.itssum.domain.user.dto.response;

import com.itssum.domain.user.entity.Organization;
import io.swagger.v3.oas.annotations.media.Schema;

/** 기부자에게 보여주는 기관 공개 정보. 사업자번호·대표자명·개업일자·좌표·이메일은 넣지 않는다. */
public record OrganizationDetailResponse(
        @Schema(description = "기관 ID", example = "2") Long organizationId,
        @Schema(description = "기관명", example = "구로지역아동센터") String name,
        @Schema(description = "기관 연락처", example = "02-1234-5678") String phone,
        @Schema(description = "기관 주소", example = "서울 구로구 가마산로 12") String address,
        @Schema(description = "상세주소", example = "2층") String addressDetail,
        @Schema(description = "기관 소개 (아직 입력 화면이 없어 비어 있을 수 있음)") String introduction
) {

    public static OrganizationDetailResponse from(Organization organization) {
        return new OrganizationDetailResponse(organization.getId(), organization.getName(), organization.getPhone(),
                organization.getAddress(), organization.getAddressDetail(), organization.getIntroduction());
    }
}
