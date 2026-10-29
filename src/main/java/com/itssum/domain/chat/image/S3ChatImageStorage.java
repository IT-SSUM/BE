package com.itssum.domain.chat.image;

import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.storage.S3ObjectStorage;
import com.itssum.global.storage.S3ObjectStorage.StoredObjectMetadata;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 채팅 전용 파일 형식과 채팅방 key 규칙. 실제 S3 처리는 global.storage에 맡긴다. */
@Component
@ConditionalOnProperty(prefix = "storage.s3", name = "bucket")
@RequiredArgsConstructor
public class S3ChatImageStorage implements ChatImageStorage {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Pattern CHAT_KEY = Pattern.compile("chat/\\d+/[0-9a-f-]{36}\\.(jpg|png|webp)");

    private final S3ObjectStorage storage;

    @Override
    public PresignedChatImageUpload presignUpload(Long chatRoomId, String contentType) {
        if (!IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "JPEG, PNG, WebP 이미지만 보낼 수 있습니다.");
        }
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> "webp";
        };
        String key = "chat/" + chatRoomId + "/" + UUID.randomUUID() + "." + extension;
        return new PresignedChatImageUpload(key, storage.presignUpload(key, contentType),
                Map.of("Content-Type", contentType));
    }

    @Override
    public void verifyUploaded(Long chatRoomId, String objectKey) {
        if (objectKey == null || !CHAT_KEY.matcher(objectKey).matches()
                || !objectKey.startsWith("chat/" + chatRoomId + "/")) {
            throw new BusinessException(ErrorCode.CHAT_IMAGE_NOT_UPLOADED);
        }
        StoredObjectMetadata object = storage.find(objectKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_IMAGE_NOT_UPLOADED));
        if (object.size() == null || object.size() == 0 || object.size() > MAX_IMAGE_BYTES
                || !IMAGE_TYPES.contains(object.contentType())) {
            throw new BusinessException(ErrorCode.CHAT_IMAGE_NOT_UPLOADED);
        }
    }

    @Override
    public String presignRead(String objectKey) {
        return storage.presignDownload(objectKey);
    }
}
