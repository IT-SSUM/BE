package com.itssum.domain.chat.image;

import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.s3.ConditionalOnS3Bucket;
import com.itssum.global.infra.s3.PresignedImageUpload;
import com.itssum.global.infra.s3.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 채팅 이미지는 채팅방 단위 key(chat/{채팅방ID}/...)를 쓴다. 형식·크기 검사와 S3 처리는 global.infra.s3에 맡긴다. */
@Component
@ConditionalOnS3Bucket
@RequiredArgsConstructor
public class S3ChatImageStorage implements ChatImageStorage {

    private static final String KEY_PREFIX = "chat";

    private final S3ImageStorage images;

    @Override
    public PresignedChatImageUpload presignUpload(Long chatRoomId, String contentType) {
        PresignedImageUpload upload = images.presignUpload(KEY_PREFIX, chatRoomId, contentType);
        return new PresignedChatImageUpload(upload.objectKey(), upload.uploadUrl(), upload.requiredHeaders());
    }

    @Override
    public void verifyUploaded(Long chatRoomId, String objectKey) {
        if (!images.verifyUploaded(KEY_PREFIX, chatRoomId, objectKey)) {
            throw new BusinessException(ErrorCode.CHAT_IMAGE_NOT_UPLOADED);
        }
    }

    @Override
    public String presignRead(String objectKey) {
        return images.presignRead(objectKey);
    }

    @Override
    public String presignThumbnailRead(String objectKey) {
        return images.presignThumbnailRead(objectKey);
    }
}
