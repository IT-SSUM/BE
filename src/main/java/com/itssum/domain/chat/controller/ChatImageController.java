package com.itssum.domain.chat.controller;

import com.itssum.domain.chat.dto.request.PresignChatImageRequest;
import com.itssum.domain.chat.image.ChatImageStorage;
import com.itssum.domain.chat.image.PresignedChatImageUpload;
import com.itssum.domain.chat.service.ChatAccessService;
import com.itssum.domain.chat.service.ChatUserResolver;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "채팅", description = "채팅방·채팅 메시지 API")
@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatImageController {

    private final ChatUserResolver userResolver;
    private final ChatAccessService accessService;
    private final ObjectProvider<ChatImageStorage> imageStorageProvider;

    @Operation(summary = "채팅 이미지 업로드 URL 발급",
            description = "채팅방 참여자에게 S3 PUT 업로드용 URL을 발급합니다. JPEG·PNG·WebP만 허용하며 URL은 10분간 유효합니다. 업로드 후 objectKey를 IMAGE 메시지의 content로 보냅니다. 저장소 미설정 시 503을 반환합니다.")
    @PostMapping("/{chatRoomId}/images/presign")
    public ApiResponse<PresignedChatImageUpload> presign(
            @Parameter(description = "채팅방 ID", example = "12") @PathVariable Long chatRoomId,
                                                          @Valid @RequestBody PresignChatImageRequest request,
                                                          Principal principal) {
        Long userId = userResolver.requireUser(principal).getId();
        accessService.requireWritableRoom(chatRoomId, userId);
        ChatImageStorage storage = imageStorageProvider.getIfAvailable();
        if (storage == null) {
            throw new BusinessException(ErrorCode.CHAT_IMAGE_STORAGE_UNAVAILABLE);
        }
        return ApiResponse.ok(SuccessCode.CHAT_IMAGE_UPLOAD_URL_ISSUED,
                storage.presignUpload(chatRoomId, request.contentType()));
    }
}
