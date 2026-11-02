package com.itssum.domain.item.controller;

import com.itssum.domain.item.dto.request.PresignItemImageRequest;
import com.itssum.domain.item.image.ItemImageKind;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import com.itssum.global.infra.s3.PresignedImageUpload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 역할 제한은 SecurityConfig 경로 규칙으로 건다 (기부 물품 사진 → DONOR, 필요 물품 사진 → ORGANIZATION). */
@Tag(name = "물품", description = "기부 물품·필요 물품 API")
@RestController
@RequiredArgsConstructor
public class ItemImageController {

    private static final String UPLOAD_DESCRIPTION = " S3 PUT 업로드용 URL을 발급합니다. JPEG·PNG·WebP만 허용하며 URL은 10분간 유효합니다. "
            + "응답의 requiredHeaders 를 업로드 요청에 그대로 넣고, 업로드 후 objectKey 를 등록·수정 요청의 사진 key 로 보냅니다. "
            + "이미지 저장소가 설정되지 않은 환경에서는 503(ITEM_503)입니다.";

    private final ItemImages itemImages;
    private final MemberReader memberReader;

    @Operation(summary = "기부 물품 사진 업로드 URL 발급",
            description = "기부 물품 등록 2/3 「사진」, 물품 수정 「물품 사진」에서 씁니다. 기부자만 호출할 수 있습니다." + UPLOAD_DESCRIPTION)
    @PostMapping("/api/donation-items/images/presign")
    public ApiResponse<PresignedImageUpload> presignDonationImage(@Valid @RequestBody PresignItemImageRequest request,
                                                                  Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_IMAGE_UPLOAD_URL_ISSUED,
                presign(ItemImageKind.DONATION, principal, request.contentType()));
    }

    @Operation(summary = "필요 물품 참고 사진 업로드 URL 발급",
            description = "필요 물품 등록 2/2 「참고 사진」, 필요 나눔 수정 「참고 사진」에서 씁니다. 기관만 호출할 수 있습니다." + UPLOAD_DESCRIPTION)
    @PostMapping("/api/needed-items/images/presign")
    public ApiResponse<PresignedImageUpload> presignNeededImage(@Valid @RequestBody PresignItemImageRequest request,
                                                                Principal principal) {
        return ApiResponse.ok(SuccessCode.ITEM_IMAGE_UPLOAD_URL_ISSUED,
                presign(ItemImageKind.NEEDED, principal, request.contentType()));
    }

    private PresignedImageUpload presign(ItemImageKind kind, Principal principal, String contentType) {
        Long userId = Long.valueOf(principal.getName());
        memberReader.activeUser(userId);
        return itemImages.presignUpload(kind, userId, contentType);
    }
}
