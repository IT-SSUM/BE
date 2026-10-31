package com.itssum.domain.user.dto.request;

import com.itssum.global.common.PhoneNumbers;
import com.itssum.global.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "기부자 프로필 수정 요청. 보내지 않은 필드는 그대로 둔다. 이메일·활동 지역은 수정할 수 없다")
public record UpdateDonorProfileRequest(
        @Schema(description = "이름", example = "최윤서")
        @Size(max = 30) @NullOrNotBlank(message = "이름을 입력해 주세요.")
        String name,
        @Schema(description = "생년월일", example = "1999-03-02")
        @Past LocalDate birthDate,
        @Schema(description = "전화번호. 하이픈은 없어도 된다", example = "010-1234-5678")
        @Pattern(regexp = PhoneNumbers.MOBILE_REGEX, message = "전화번호 형식이 올바르지 않습니다.")
        String phone
) {
}
