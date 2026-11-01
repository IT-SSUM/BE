package com.itssum.domain.auth.dto.request;

import com.itssum.domain.user.entity.TermsType;
import com.itssum.global.common.PhoneNumbers;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "기관 회원가입 요청")
public record SignUpOrganizationRequest(
        @Schema(description = "이메일 인증 확인 API 가 발급한 토큰")
        @NotBlank String emailVerificationToken,
        @Schema(description = "사업자 확인 API 가 발급한 토큰")
        @NotBlank String businessVerificationToken,
        @Schema(description = "로그인 이메일 (인증한 이메일과 같아야 함)", example = "org@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "비밀번호 8~20자, 영문·숫자 포함, 공백 불가", example = "abcd1234")
        @NotBlank @Size(min = 8, max = 20)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S+$",
                message = "비밀번호는 영문과 숫자를 모두 포함하고 공백 없이 입력해 주세요.")
        String password,
        @Schema(description = "기관명", example = "구로지역아동센터")
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "대표자 이름 (사업자 확인 때와 같아야 함)", example = "김수현")
        @NotBlank @Size(max = 50) String representativeName,
        @Schema(description = "사업자등록번호 10자리. 하이픈은 없어도 된다 (사업자 확인 때와 같아야 함)", example = "123-45-67890")
        @NotBlank @Pattern(regexp = "^\\d{3}-?\\d{2}-?\\d{5}$", message = "사업자등록번호 10자리를 입력해 주세요.")
        String businessNumber,
        @Schema(description = "개업일자 (사업자 확인 때와 같아야 함)", example = "2015-03-02")
        @NotNull @PastOrPresent LocalDate openingDate,
        @Schema(description = "기관 전화번호. 지역번호 포함, 하이픈은 없어도 된다", example = "02-1234-5678")
        @NotBlank @Pattern(regexp = PhoneNumbers.CONTACT_REGEX, message = "전화번호 형식이 올바르지 않습니다.")
        String phone,
        @Schema(description = "기관 주소 (우편번호 검색 결과의 도로명 주소)", example = "서울 구로구 가마산로 12")
        @NotBlank @Size(max = 255) String address,
        @Schema(description = "상세주소 (선택)", example = "2층")
        @Size(max = 100) String addressDetail,
        @Schema(description = "동의한 약관. SERVICE, PRIVACY, ORG_ELIGIBILITY 필수",
                example = "[\"SERVICE\", \"PRIVACY\", \"ORG_ELIGIBILITY\"]")
        @NotNull List<TermsType> agreedTerms
) {
}
