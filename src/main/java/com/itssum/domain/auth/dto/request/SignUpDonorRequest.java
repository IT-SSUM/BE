package com.itssum.domain.auth.dto.request;

import com.itssum.domain.user.entity.TermsType;
import com.itssum.global.common.PhoneNumbers;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "기부자 회원가입 요청")
public record SignUpDonorRequest(
        @Schema(description = "이메일 인증 확인 API 가 발급한 토큰")
        @NotBlank String emailVerificationToken,
        @Schema(description = "로그인 이메일 (인증한 이메일과 같아야 함)", example = "donor@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "비밀번호 8~20자, 영문·숫자 포함, 공백 불가", example = "abcd1234")
        @NotBlank @Size(min = 8, max = 20)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S+$",
                message = "비밀번호는 영문과 숫자를 모두 포함하고 공백 없이 입력해 주세요.")
        String password,
        @Schema(description = "이름", example = "최윤서")
        @NotBlank @Size(max = 30) String name,
        @Schema(description = "생년월일", example = "1999-03-02")
        @NotNull @Past LocalDate birthDate,
        @Schema(description = "전화번호. 하이픈은 없어도 된다", example = "010-1234-5678")
        @NotBlank @Pattern(regexp = PhoneNumbers.MOBILE_REGEX, message = "전화번호 형식이 올바르지 않습니다.")
        String phone,
        @Schema(description = "활동 지역 ID (선택). 파일럿 지역 경기 부천시(1)로 고정이며, 생략해도 1로 저장한다. "
                + "다른 지역은 REGION_400, 없는 ID는 REGION_404", example = "1")
        Long regionId,
        @Schema(description = "동의한 약관. SERVICE, PRIVACY 필수", example = "[\"SERVICE\", \"PRIVACY\"]")
        @NotNull List<TermsType> agreedTerms
) {
}
