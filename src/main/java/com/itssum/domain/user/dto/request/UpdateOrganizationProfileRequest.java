package com.itssum.domain.user.dto.request;

import com.itssum.global.common.PhoneNumbers;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "기관 정보 수정 요청. 보내지 않은 필드는 그대로 둔다. 대표자명·사업자번호·개업일자·이메일은 수정할 수 없다")
public record UpdateOrganizationProfileRequest(
        @Schema(description = "기관명", example = "구로지역아동센터")
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "기관명을 입력해 주세요.")
        String name,
        @Schema(description = "기관 전화번호. 지역번호 포함, 하이픈은 없어도 된다", example = "02-1234-5678")
        @Pattern(regexp = PhoneNumbers.CONTACT_REGEX, message = "전화번호 형식이 올바르지 않습니다.")
        String phone,
        @Schema(description = "기관 주소 (우편번호 검색 결과의 도로명 주소). 보내면 상세주소도 이 요청의 값으로 바뀐다",
                example = "서울 구로구 가마산로 12")
        @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "주소를 입력해 주세요.")
        String address,
        @Schema(description = "상세주소. 빈 값이면 비운다", example = "2층")
        @Size(max = 100) String addressDetail
) {
}
