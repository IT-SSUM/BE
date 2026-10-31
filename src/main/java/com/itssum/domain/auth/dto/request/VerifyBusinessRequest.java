package com.itssum.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "사업자 확인 요청 (국세청 진위확인)")
public record VerifyBusinessRequest(
        @Schema(description = "사업자등록번호 10자리. 하이픈은 없어도 된다", example = "123-45-67890")
        @NotBlank @Pattern(regexp = "^\\d{3}-?\\d{2}-?\\d{5}$", message = "사업자등록번호 10자리를 입력해 주세요.")
        String businessNumber,
        @Schema(description = "개업일자", example = "2015-03-02")
        @NotNull @PastOrPresent LocalDate openingDate,
        @Schema(description = "대표자 이름 (기관 정보 단계에서 입력한 값)", example = "김수현")
        @NotBlank @Size(max = 50) String representativeName
) {
}
